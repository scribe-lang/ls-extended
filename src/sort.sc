let sorting = @import("std/sorting");

let core = @import("./core");

let static reverse = false;

let setRev = fn() { reverse = true; };
let isRev = fn(): i1 { return reverse; };

let name = fn(a: &const core.StatInfo, b: &const core.StatInfo): i32 {
	let res = sorting.strCmp(a.name, b.name);
	if isRev() { res = -res; }
	return res;
};

let ext = fn(a: &const core.StatInfo, b: &const core.StatInfo): i32 {
	let res = sorting.strCmp(a.ext, b.ext);
	if isRev() { res = -res; }
	return res;
};

let mtime = fn(a: &const core.StatInfo, b: &const core.StatInfo): i32 {
	let res = sorting.i64Cmp(a.linkst.st_mtime, b.linkst.st_mtime);
	if isRev() { res = -res; }
	return res;
};

let size = fn(a: &const core.StatInfo, b: &const core.StatInfo): i32 {
	let res = sorting.i64Cmp(a.linkst.st_size, b.linkst.st_size);
	if isRev() { res = -res; }
	return res;
};
