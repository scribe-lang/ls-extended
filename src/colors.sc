let comptime RESET = "\e[0m".data; // 0

let comptime RED = "\e[0;31m".data; // r
let comptime GREEN = "\e[0;32m".data; // g
let comptime YELLOW = "\e[0;33m".data; // y
let comptime BLUE = "\e[0;34m".data; // b
let comptime MAGENTA = "\e[0;35m".data; // m
let comptime CYAN = "\e[0;36m".data; // c
let comptime WHITE = "\e[0;37m".data; // w

let comptime BOLD_RED = "\e[1;31m".data; // br
let comptime BOLD_GREEN = "\e[1;32m".data; // bg
let comptime BOLD_YELLOW = "\e[1;33m".data; // by
let comptime BOLD_BLUE = "\e[1;34m".data; // bb
let comptime BOLD_MAGENTA = "\e[1;35m".data; // bm
let comptime BOLD_CYAN = "\e[1;36m".data; // bc
let comptime BOLD_WHITE = "\e[1;37m".data; // bw

let comptime PRIMARY = "\e[0;35m".data; // p
let comptime SECONDARY = "\e[0;33m".data; // s
let comptime TERTIARY = "\e[0;37m".data; // t
let comptime EXTRA = "\e[0;36m".data; // e

let get = fn(id: StringRef): *const i8 {
	if id == "0" { return RESET; }

	if id == "r" { return RED; }
	if id == "g" { return GREEN; }
	if id == "y" { return YELLOW; }
	if id == "b" { return BLUE; }
	if id == "m" { return MAGENTA; }
	if id == "c" { return CYAN; }
	if id == "w" { return WHITE; }

	if id == "br" { return BOLD_RED; }
	if id == "bg" { return BOLD_GREEN; }
	if id == "by" { return BOLD_YELLOW; }
	if id == "bb" { return BOLD_BLUE; }
	if id == "bm" { return BOLD_MAGENTA; }
	if id == "bc" { return BOLD_CYAN; }
	if id == "bw" { return BOLD_WHITE; }


	if id == "p" { return PRIMARY; }
	if id == "s" { return SECONDARY; }
	if id == "t" { return TERTIARY; }
	if id == "e" { return EXTRA; }

	return nil;
};