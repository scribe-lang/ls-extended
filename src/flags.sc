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
	args.add("all", "a").setHelp("Show all files/folders, including hidden");
	args.add("dir", "d").setHelp("Show only folders/directories");
	args.add("files", "f").setHelp("Show only files");
	args.add("human", "H").setHelp("Show all sizes in human readable format (only usable with -l)");
	args.add("inode", "i").setHelp("Show inode numbers of all files/folders (only usable with -l)");
	args.add("iso-time", "I").setHelp("Show time in ISO-Long format (only usable with -l)");
	args.add("list", "l").setHelp("Show the files/folders in a list format with their various information");
	args.add("no-col", "N").setHelp("Disable colored output [TODO]");
	args.add("rev", "r").setHelp("Reverse the sorting order");
	args.add("rev", "r").setHelp("Reverse the sorting order");
	args.add("size", "S").setHelp("Sort by size");
	args.add("time", "T").setHelp("Sort by time");
	args.add("vers", "V").setHelp("Sort by version [TODO]");
	args.add("ext", "X").setHelp("Sort by extension");
	args.add("sort-dir", "s").setHelp("Sorts the output by directories first");
	args.add("one", "1").setHelp("Force output to be one entry per line");
};

// must be called after ArgParser.parse()
let getMask = fn(args: &argparse.ArgParser): u32 {
	let mask: u32 = 0;
	if args.has("all") { mask |= A; }
	if args.has("dir") { mask |= D; }
	if args.has("files") { mask |= F; }
	if args.has("human") { mask |= CAPS_H; }
	if args.has("inode") { mask |= I; }
	if args.has("iso-time") { mask |= CAPS_I; }
	if args.has("list") { mask |= L; }
	if args.has("no-col") { mask |= CAPS_N; }
	if args.has("rev") { mask |= R; }
	if args.has("size") { mask |= CAPS_S; }
	if args.has("time") { mask |= CAPS_T; }
	if args.has("vers") { mask |= CAPS_V; }
	if args.has("ext") { mask |= CAPS_X; }
	if args.has("sort-dir") { mask |= S; }
	if args.has("one") { mask |= ONE; }
	return mask;
};