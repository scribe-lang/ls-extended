let c = @import("std/c");
let io = @import("std/io");
let vec = @import("std/vec");

let core = @import("./core");
let clrs = @import("./colors");
let flags = @import("./flags");

let time = c.time;
let ioctl = c.ioctl;

let setWidths = fn(locs: &vec.Vec(core.StatInfo), rows: i32, cols: i32) {
	let count = locs.len();
	let i: u64 = 0;
	for let c = 0; c < cols; ++c {
		let maxlen = 0;
		let tmpi = i;
		for let r = 0; r < rows; ++r {
			if i >= count { continue; }
			let stats = locs[i];
			if maxlen < stats.namelen + 1 { maxlen = stats.namelen + 1; }
			++i;
		}
		i = tmpi;
		for let r = 0; r < rows; ++r {
			if i >= count { continue; }
			let stats = locs[i];
			stats.width += maxlen;
			++i;
		}
	}
};

let calcNumRowsCols = fn(locs: &vec.Vec(core.StatInfo), maxlen: &core.MaxLen, ws: &const ioctl.winsize,
			 rows: &i32, cols: &i32, mask: u32) {
	let count = locs.len();
	if maxlen.name == 0 { maxlen.name = 1; }
	cols = ws.ws_col / maxlen.name;
	if cols <= 0 { cols = 1; }
	rows = count / cols;
	if rows <= 0 { rows = 1; }
	if cols > count { rows = 1; cols = count; return; }
	if mask & flags.ONE { rows = count; cols = 1; return; }

	while rows * cols < count { ++cols; }

	let totlen = 0;
	let i: u64 = 0;
	let can_reduce_rows = -1;

	while true {
		totlen = 0;
		i = 0;
		for let c = 0; c < cols; ++c {
			let maxlen = 0;
			let lastadd = 1;
			if c == cols - 1 { lastadd = 0; }
			for let r = 0; r < rows; ++r {
				if i >= count { continue; }
				let stats = locs[i];
				if maxlen < stats.namelen + stats.width + lastadd {
					maxlen = stats.namelen + stats.width + lastadd;
				}
				++i;
			}
			totlen += maxlen;
		}
		--totlen; // for \n
		if totlen >= ws.ws_col {
			if can_reduce_rows != -1 { can_reduce_rows = 0; }
			--cols;
			++rows;
			continue;
		}
		if can_reduce_rows != 0 && rows > 1 && (rows - 1) * (cols - 1) >= count {
			can_reduce_rows = 1;
			++cols;
			--rows;
			continue;
		}
		break;
	}
};

let basic = fn(locs: &vec.Vec(core.StatInfo), ws: &const ioctl.winsize, maxlen: *core.MaxLen, mask: u32) {
	let count = locs.len();
	if !count { return; }
	if @as(u64, maxlen) == nil {
		let stats = locs[0];
		if stats.linkst.isDir() {
			io.print(clrs.BLUE, stats.name, clrs.RESET);
		} elif stats.linkst.isLink() {
			if stats.linkdead { io.print(clrs.RED, stats.name, clrs.RESET); }
			else { io.print(clrs.YELLOW, stats.name, clrs.RESET); }
		} else {
			io.print(clrs.GREEN, stats.name, clrs.RESET);
		}
		io.println();
		return;
	}
	let rows = 0, cols = 0;
	calcNumRowsCols(locs, *maxlen, ws, rows, cols, mask);
	setWidths(locs, rows, cols);

	let item_line_ctr = 0;

	for let R = 0; R < rows; ++R {
		for let C = 0; C < cols; ++C {
			let i: u64 = C * rows + R;
			if i >= count { continue; }
			let stats = locs[i];
			let cspace = stats.width;
			if C == cols - 1 { cspace = -1; }
			if stats.linkst.isDir() {
				io.fprintf(io.stdout, r"%s%-*s%s", clrs.BLUE, cspace, stats.name.cStr(), clrs.RESET);
			} elif stats.linkst.isLink() {
				if stats.linkdead {
					io.fprintf(io.stdout, r"%s%-*s%s", clrs.RED, cspace, stats.name.cStr(), clrs.RESET);
				} else {
					io.fprintf(io.stdout, r"%s%-*s%s", clrs.YELLOW, cspace, stats.name.cStr(), clrs.RESET);
				}
			} else {
				if stats.st.isXUSR() {
					io.fprintf(io.stdout, r"%s%-*s%s", clrs.GREEN, cspace, stats.name.cStr(), clrs.RESET);
				} else {
					io.fprintf(io.stdout, r"%s%-*s%s", clrs.RESET, cspace, stats.name.cStr(), clrs.RESET);
				}
			}
		}
		io.println();
	}
};

let list = fn(locs: &vec.Vec(core.StatInfo), ws: &const ioctl.winsize, maxlen: *core.MaxLen, mask: u32) {
	let count = locs.len();
	for let i: u64 = 0; i < count; ++i {
		let stats = locs[i];

		// inode number
		if mask & flags.I {
			let inodespace = 0;
			if @as(u64, maxlen) != nil { inodespace = maxlen.inode; }
			io.fprintf(io.stdout, r"%-*llu ", inodespace, stats.st.st_ino);
		}

		// dir/link/file
		if stats.st.isDir() { io.fprintf(io.stdout, r"%sd", clrs.BLUE); }
		elif stats.st.isLink() { io.fprintf(io.stdout, r"%sl", clrs.BLUE); }
		else { io.fprintf(io.stdout, r"%s-", clrs.BLUE); }

		if stats.st.isRUSR() { io.fprintf(io.stdout, r"%sr", clrs.MAGENTA); }
		else { io.fprintf(io.stdout, r"%s-", clrs.RESET); }
		if stats.st.isWUSR() { io.fprintf(io.stdout, r"%sw", clrs.CYAN); }
		else { io.fprintf(io.stdout, r"%s-", clrs.RESET); }
		if stats.st.isXUSR() { io.fprintf(io.stdout, r"%sx", clrs.RED); }
		else { io.fprintf(io.stdout, r"%s-", clrs.RESET); }
		if stats.st.isRGRP() { io.fprintf(io.stdout, r"%sr", clrs.MAGENTA); }
		else { io.fprintf(io.stdout, r"%s-", clrs.RESET); }
		if stats.st.isWGRP() { io.fprintf(io.stdout, r"%sw", clrs.CYAN); }
		else { io.fprintf(io.stdout, r"%s-", clrs.RESET); }
		if stats.st.isXGRP() { io.fprintf(io.stdout, r"%sx", clrs.RED); }
		else { io.fprintf(io.stdout, r"%s-", clrs.RESET); }
		if stats.st.isROTH() { io.fprintf(io.stdout, r"%sr", clrs.MAGENTA); }
		else { io.fprintf(io.stdout, r"%s-", clrs.RESET); }
		if stats.st.isWOTH() { io.fprintf(io.stdout, r"%sw", clrs.CYAN); }
		else { io.fprintf(io.stdout, r"%s-", clrs.RESET); }
		if stats.st.isXOTH() { io.fprintf(io.stdout, r"%sx", clrs.RED); }
		else { io.fprintf(io.stdout, r"%s-", clrs.RESET); }
		if stats.st.isVTX() { io.fprintf(io.stdout, r"%ss", clrs.RED); }
		else { io.fprintf(io.stdout, r"%s ", clrs.RESET); }
		io.print(" ");

		// links
		if @as(u64, maxlen) == nil {
			io.fprintf(io.stdout, r"%s%-*d ", clrs.RESET, 0, stats.st.st_nlink);
		} else {
			io.fprintf(io.stdout, r"%s%-*d ", clrs.RESET, maxlen.links, stats.st.st_nlink);
		}
		// user & group
		if @as(u64, maxlen) == nil {
			io.fprintf(io.stdout, r"%s%-*s ", clrs.GREEN, 0, stats.user.cStr());
			io.fprintf(io.stdout, r"%s%-*s ", clrs.GREEN, 0, stats.group.cStr());
		} else {
			io.fprintf(io.stdout, r"%s%-*s ", clrs.GREEN, maxlen.user, stats.user.cStr());
			io.fprintf(io.stdout, r"%s%-*s ", clrs.GREEN, maxlen.group, stats.group.cStr());
		}
		// file size
		if @as(u64, maxlen) == nil {
			io.fprintf(io.stdout, r"%s%-*s ", clrs.YELLOW, 0, stats.size.cStr());
		} else {
			io.fprintf(io.stdout, r"%s%-*s ", clrs.YELLOW, maxlen.size, stats.size.cStr());
		}

		// last modified time
		let mtime: @array(i8, 30);
		if mask & flags.CAPS_I {
			// long-iso format
			time.strftime(mtime, 30, r"%Y-%m-%d %H:%M", time.localtime(&stats.st.st_mtime));
		} else {
			time.strftime(mtime, 30, r"%h %e %H:%M", time.localtime(&stats.st.st_mtime));
		}
		io.fprintf(io.stdout, r"%s%s ", clrs.MAGENTA, mtime);

		// file/folder name
		if stats.linkst.isDir() {
			io.fprintf(io.stdout, r"%s%-*s%s", clrs.BLUE, stats.width, stats.name.cStr(), clrs.RESET);
		} elif stats.st.isLink() {
			if stats.linkdead {
				io.fprintf(io.stdout, r"%s%-*s%s", clrs.RED, stats.width, stats.name.cStr(), clrs.RESET);
			} else {
				io.fprintf(io.stdout, r"%s%-*s%s", clrs.YELLOW, stats.width, stats.name.cStr(), clrs.RESET);
			}
		} else {
			if stats.st.isXUSR() {
				io.fprintf(io.stdout, r"%s%-*s%s", clrs.BOLD_GREEN, stats.width, stats.name.cStr(), clrs.RESET);
			} else {
				io.fprintf(io.stdout, r"%s%-*s%s", clrs.RESET, stats.width, stats.name.cStr(), clrs.RESET);
			}
		}

		// link info for links
		if stats.st.isLink() {
			io.fprintf(io.stdout, r" %s-> %s%s%s", clrs.MAGENTA, clrs.CYAN, stats.linkloc.cStr(), clrs.RESET);
			if stats.linkdead {
				io.fprintf(io.stdout, r" %s[%sdead link%s]%s", clrs.YELLOW, clrs.RED, clrs.YELLOW, clrs.RESET);
			}
		}

		io.println(clrs.RESET);
	}
};
