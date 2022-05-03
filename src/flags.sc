let string = @import("std/string");
let argparse = @import("std/argparse");

// Option bit masks
let A: const u32 = 1 << 0;
let D: const u32 = 1 << 1;
let F: const u32 = 1 << 2;
let G: const u32 = 1 << 3;
let H: const u32 = 1 << 4; // help
let CAPS_H: const u32 = 1 << 5; // human readable
let I: const u32 = 1 << 6;
let CAPS_I: const u32 = 1 << 7; // long-iso time format
let L: const u32 = 1 << 8;
let CAPS_N: const u32 = 1 << 9; // no colors
let R: const u32 = 1 << 10; // reverse sort order
let S: const u32 = 1 << 11; // show dirs first
let CAPS_S: const u32 = 1 << 12; // size sort
let CAPS_T: const u32 = 1 << 13; // time sort
let CAPS_V: const u32 = 1 << 14; // version sort
let CAPS_X: const u32 = 1 << 15; // extension sort
let ONE: const u32 = 1 << 16;

let addToArgParser = fn(args: &argparse.ArgParser) {
	args.add(ref"all", ref"a").setHelp(ref"Show all files/folders, including hidden");
	args.add(ref"dir", ref"d").setHelp(ref"Show only folders/directories");
	args.add(ref"files", ref"f").setHelp(ref"Show only files");
	args.add(ref"human", ref"H").setHelp(ref"Show all sizes in human readable format (only usable with -l)");
	args.add(ref"inode", ref"i").setHelp(ref"Show inode numbers of all files/folders (only usable with -l)");
	args.add(ref"iso-time", ref"I").setHelp(ref"Show time in ISO-Long format (only usable with -l)");
	args.add(ref"list", ref"l").setHelp(ref"Show the files/folders in a list format with their various information");
	args.add(ref"no-col", ref"N").setHelp(ref"Disable colored output [TODO]");
	args.add(ref"rev", ref"r").setHelp(ref"Reverse the sorting order");
	args.add(ref"rev", ref"r").setHelp(ref"Reverse the sorting order");
	args.add(ref"size", ref"S").setHelp(ref"Sort by size");
	args.add(ref"time", ref"T").setHelp(ref"Sort by time");
	args.add(ref"vers", ref"V").setHelp(ref"Sort by version [TODO]");
	args.add(ref"ext", ref"X").setHelp(ref"Sort by extension");
	args.add(ref"sort-dir", ref"s").setHelp(ref"Sorts the output by directories first");
	args.add(ref"one", ref"1").setHelp(ref"Force output to be one entry per line");
};

// must be called after ArgParser.parse()
let getMask = fn(args: &argparse.ArgParser): u32 {
	let mask: u32 = 0;
	if args.has(ref"all") { mask |= A; }
	if args.has(ref"dir") { mask |= D; }
	if args.has(ref"files") { mask |= F; }
	if args.has(ref"human") { mask |= CAPS_H; }
	if args.has(ref"inode") { mask |= I; }
	if args.has(ref"iso-time") { mask |= CAPS_I; }
	if args.has(ref"list") { mask |= L; }
	if args.has(ref"no-col") { mask |= CAPS_N; }
	if args.has(ref"rev") { mask |= R; }
	if args.has(ref"size") { mask |= CAPS_S; }
	if args.has(ref"time") { mask |= CAPS_T; }
	if args.has(ref"vers") { mask |= CAPS_V; }
	if args.has(ref"ext") { mask |= CAPS_X; }
	if args.has(ref"sort-dir") { mask |= S; }
	if args.has(ref"one") { mask |= ONE; }
	return mask;
};