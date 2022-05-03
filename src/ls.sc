let c = @import("std/c");
let io = @import("std/io");
let os = @import("std/os");
let err = @import("std/err");
let vec = @import("std/vec");
let utils = @import("std/utils");
let string = @import("std/string");

let core = @import("./core");
let sort = @import("./sort");
let disp = @import("./disp");
let flags = @import("./flags");

let stat = c.stat;
let ioctl = c.ioctl;
let dirent = c.dirent;
let unistd = c.unistd;

let comptime MAX_LINK_JUMP_COUNT: const i32 = 32;

let formatFileSize = fn(size: u64, res: &string.String) {
	let size_res: f64 = size;
	if size < 1024 {
		res = size;
		res += "B";
		return;
	}

	let divcount = 0;
	while size_res > 1024 && divcount < 4 {
		size_res /= 1024;
		++divcount;
	}

	let prec = string.getPrecision();
	string.setPrecision(1);
	res += size_res;
	string.setPrecision(prec);

	if divcount == 1 { res += "K"; }
	elif divcount == 2 { res += "M"; }
	elif divcount == 3 { res += "G"; }
	elif divcount == 4 { res += "T"; }
};

let getStats = fn(path: &const string.String, stats: &core.StatInfo): i1 {
	let currdir = os.getCWD();
	defer currdir.deinit();
	defer os.setCWD(currdir.cStr());

	let tmp_st = stat.new();
	let tmp_res = stat.lstat(path.cStr(), &tmp_st);

	if stats.linkjumps == 0 {
		if tmp_res {
			err.push(c.errno, "Failed to stat '", path, "', error: ", c.strerror(c.errno));
			return false;
		}
		stats.st.copy(tmp_st);
	}

	if stats.linkjumps >= 1 && tmp_res {
		stats.linkdead = true;
		stats.linkst.copy(stats.st);
		return true;
	}

	if tmp_st.isLink() {
		++stats.linkjumps;
		if stats.linkjumps >= MAX_LINK_JUMP_COUNT { return true; }

		let buf: @array(i8, os.PATH_MAX);
		let len = unistd.readlink(path.cStr(), buf, os.PATH_MAX - 1);
		if len < 0 {
			err.push(c.errno, "Failed to readlink '", path, "', error: ", c.strerror(c.errno));
			return false;
		}
		buf[len] = 0;
		if stats.linkjumps == 1 { stats.linkloc = buf; }
		let jmpdir = os.dirName(buf);
		defer jmpdir.deinit();
		let jmpfile = os.baseName(buf);
		defer jmpfile.deinit();
		let cd_res = os.setCWD(jmpdir.cStr());
		if cd_res {
			stats.linkdead = true;
			stats.linkst.copy(stats.st);
			return true;
		}
		// no direct return because defer will be applied before return
		let res = getStats(jmpfile, stats);
		return res;
	}
	stats.linkst.copy(tmp_st);
	return true;
};

let updateDataAndMaxLen = fn(stats: &core.StatInfo, maxlen: *core.MaxLen, mask: u32) {
	let len = 0;
	if !(mask & flags.L) { return; }

	if @as(u64, maxlen) != nil {
		len = utils.countUIntDigits(stats.st.st_nlink);
		if maxlen.links < len { maxlen.links = len; }
	}

	let usr = c.pwd.getpwuid(stats.st.st_uid);
	let grp = c.grp.getgrgid(stats.st.st_gid);

	if @as(u64, usr) == nil {
		stats.user = stats.st.st_uid;
		if @as(u64, maxlen) != nil { len = stats.user.len(); }
	} else {
		stats.user = usr.pw_name;
		if @as(u64, maxlen) != nil { len = core.utf8Strlen(usr.pw_name); }
	}
	if @as(u64, maxlen) != nil && maxlen.user < len { maxlen.user = len; }

	if @as(u64, grp) == nil {
		stats.group = stats.st.st_gid;
		if @as(u64, maxlen) != nil { len = stats.group.len(); }
	} else {
		stats.group = grp.gr_name;
		if @as(u64, maxlen) != nil { len = core.utf8Strlen(grp.gr_name); }
	}
	if @as(u64, maxlen) != nil && maxlen.group < len { maxlen.group = len; }

	if @as(u64, maxlen) != nil && (mask & flags.I) {
		len = utils.countUIntDigits(stats.st.st_ino);
		if maxlen.inode < len { maxlen.inode = len; }
	}

	if mask & flags.CAPS_H {
		formatFileSize(stats.st.st_size, stats.size);
		if @as(u64, maxlen) != nil { len = stats.size.len(); }
	} else {
		stats.size = stats.st.st_size;
		if @as(u64, maxlen) != nil { len = utils.countIntDigits(stats.st.st_size); }
	}
	if @as(u64, maxlen) != nil && maxlen.size < len { maxlen.size = len; }
};

let genFileVec = fn(dir: *dirent.DIR, maxlen: &core.MaxLen, baseloc: *const i8, mask: u32): vec.Vec(core.StatInfo) {
	let di: *dirent.dirent = nil;
	let locs = vec.new(core.StatInfo, true);
	let dirlocs = vec.new(core.StatInfo, false); // only used if mask & flags.S == true
	defer dirlocs.deinit();

	maxlen.clear();

	while @as(u64, di = dirent.readdir(dir)) != nil {
		if mask & flags.D || mask & flags.F {
			if di.d_type == dirent.DT_DIR && !(mask & flags.D) { continue; }
			if di.d_type != dirent.DT_DIR && !(mask & flags.F) { continue; }
		}
		if c.strcmp(di.d_name, ".") == 0 || c.strcmp(di.d_name, "..") == 0 {
			continue;
		}

		let stats = core.newStatInfo();
		let loc = string.from(baseloc);
		defer loc.deinit();
		loc += di.d_name;

		if !getStats(loc, stats) {
			dirlocs.setManaged(true);
			locs.clear();
			return locs;
		}

		core.splitFile(di.d_name, stats.name, stats.ext);
		if !(mask & flags.L) {
			let spcs = 2;
			if stats.st.isLink() {
				stats.name += "@";
				--spcs;
			}
			if stats.linkst.isDir() {
				stats.name += "/";
				--spcs;
			}
			if spcs == 2 { stats.name += "  "; }
			if spcs == 1 { stats.name += " "; }
		}

		stats.namelen = core.utf8Strlen(stats.name.cStr());
		if maxlen.name < stats.namelen { maxlen.name = stats.namelen; }
		stats.width = core.getExtraSpaces(stats.name.cStr());
		updateDataAndMaxLen(stats, &maxlen, mask);
		if stats.linkst.isDir() && (mask & flags.S) { dirlocs.push(stats); }
		else { locs.push(stats); }
	}

	if mask & flags.CAPS_T {
		if mask & flags.S { dirlocs.sort(sort.mtime); }
		locs.sort(sort.mtime);
	} elif mask & flags.CAPS_X {
		if mask & flags.S { dirlocs.sort(sort.ext); }
		locs.sort(sort.ext);
	} elif mask & flags.CAPS_S {
		if mask & flags.S { dirlocs.sort(sort.size); }
		locs.sort(sort.size);
	} else {
		if mask & flags.S { dirlocs.sort(sort.name); }
		locs.sort(sort.name);
	}

	if mask & flags.S {
		for d in dirlocs.eachRev() {
			locs.insert(d, 0);
		}
	}
	return locs;
};

let run = fn(entry: string.StringRef, mask: u32, entrycount: u64, ws: &const ioctl.winsize): i1 {
	let fentry = string.from(entry);
	defer fentry.deinit();

	if !(mask & flags.L) {
		mask &= ~flags.G;
		mask &= ~flags.CAPS_H;
		mask &= ~flags.I;
		mask &= ~flags.CAPS_I;
	}

	let tmp_st = stat.new();
	let tmp_st_res = stat.stat(fentry.cStr(), &tmp_st);
	if tmp_st_res {
		err.push(c.errno, "Failed to stat '", fentry, "', error: ", c.strerror(c.errno));
		return false;
	}
	if !tmp_st.isDir() {
		let stats = core.newStatInfo();
		defer stats.deinit();
		if !getStats(fentry, stats) { return false; }
		core.splitFile(fentry.cStr(), stats.name, stats.ext);
		if !(mask & flags.L) {
			let spcs = 2;
			if stats.st.isLink() {
				stats.name += "@";
				--spcs;
			}
			if stats.linkst.isDir() {
				stats.name += "/";
				--spcs;
			}
			if spcs == 2 { stats.name += "  "; }
			if spcs == 1 { stats.name += " "; }
		}
		updateDataAndMaxLen(stats, nil, mask);
		let locs = vec.new(core.StatInfo, false);
		defer locs.deinit();
		locs.push(stats);
		if mask & flags.L { disp.list(locs, ws, nil, mask); }
		else { disp.basic(locs, ws, nil, mask); }
		return true;
	}
	if fentry[fentry.len() - 1] != '/' {
		fentry.append('/');
	}

	let currdir = os.getCWD();
	defer currdir.deinit();
	let cd_res = os.setCWD(fentry.cStr());
	if cd_res {
		err.push(c.errno, "Failed to open directory '", fentry, "', error: ", c.strerror(c.errno));
		return false;
	}

	let dir = dirent.opendir(".");

	if @as(u64, dir) == nil {
		err.push(c.errno, "Failed to open directory '", fentry, "', error: ", c.strerror(c.errno));
		return false;
	}

	let maxlen = core.newMaxLen();
	let locs = genFileVec(dir, maxlen, "./", mask);
	defer locs.deinit();
	dirent.closedir(dir);

	if locs.isEmpty() { return false; }
	if !(mask & flags.L) { disp.basic(locs, ws, &maxlen, mask); }
	else { disp.list(locs, ws, &maxlen, mask); }
	return true;
};