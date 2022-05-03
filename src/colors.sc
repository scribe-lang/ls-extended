let string = @import("std/string");

let RESET = "\e[0m"; // 0

let RED = "\e[0;31m"; // r
let GREEN = "\e[0;32m"; // g
let YELLOW = "\e[0;33m"; // y
let BLUE = "\e[0;34m"; // b
let MAGENTA = "\e[0;35m"; // m
let CYAN = "\e[0;36m"; // c
let WHITE = "\e[0;37m"; // w

let BOLD_RED = "\e[1;31m"; // br
let BOLD_GREEN = "\e[1;32m"; // bg
let BOLD_YELLOW = "\e[1;33m"; // by
let BOLD_BLUE = "\e[1;34m"; // bb
let BOLD_MAGENTA = "\e[1;35m"; // bm
let BOLD_CYAN = "\e[1;36m"; // bc
let BOLD_WHITE = "\e[1;37m"; // bw

let PRIMARY = "\e[0;35m"; // p
let SECONDARY = "\e[0;33m"; // s
let TERTIARY = "\e[0;37m"; // t
let EXTRA = "\e[0;36m"; // e

let get = fn(id: string.StringRef): *const i8 {
	if id == ref"0" { return RESET; }

	if id == ref"r" { return RED; }
	if id == ref"g" { return GREEN; }
	if id == ref"y" { return YELLOW; }
	if id == ref"b" { return BLUE; }
	if id == ref"m" { return MAGENTA; }
	if id == ref"c" { return CYAN; }
	if id == ref"w" { return WHITE; }

	if id == ref"br" { return BOLD_RED; }
	if id == ref"bg" { return BOLD_GREEN; }
	if id == ref"by" { return BOLD_YELLOW; }
	if id == ref"bb" { return BOLD_BLUE; }
	if id == ref"bm" { return BOLD_MAGENTA; }
	if id == ref"bc" { return BOLD_CYAN; }
	if id == ref"bw" { return BOLD_WHITE; }


	if id == ref"p" { return PRIMARY; }
	if id == ref"s" { return SECONDARY; }
	if id == ref"t" { return TERTIARY; }
	if id == ref"e" { return EXTRA; }

	return nil;
};