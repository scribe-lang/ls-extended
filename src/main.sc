let c = @import("std/c");
let io = @import("std/io");
let err = @import("std/err");
let argparse = @import("std/argparse");

let ls = @import("./ls");
let core = @import("./core");
let sort = @import("./sort");
let flags = @import("./flags");
let colors = @import("./colors");

let ioctl = c.ioctl;
let unistd = c.unistd;

let main = fn(argc: i32, argv: **i8): i32 {
	err.init();
	defer err.deinit();

	let args = argparse.new(argc, argv);
	defer args.deinit();
	args.add(ref"help", ref"h").setHelp(ref"prints program usage format");
	args.add(ref"version", ref"v").setHelp(ref"prints program version");
	flags.addToArgParser(args);

	if !args.parse() {
		io.println("Failed to parse command line arguments");
		if err.present() {
			io.println("Error: ", err.pop());
		}
		return 1;
	}
	if args.has(ref"help") {
		args.printHelp(io.stdout);
		return 0;
	}
	if args.has(ref"version") {
		io.println("Ls Extended v0.0.1, built with Scribe Compiler v", @compilerID());
		return 0;
	}
	let entries = args.getAllArgIdxFrom(1);
	defer entries.deinit();

	if entries.isEmpty() { entries.pushVal(ref"."); }

	let ws: ioctl.winsize;
	ioctl.ioctl(unistd.STDOUT_FILENO, ioctl.TIOCGWINSZ, &ws);

	let flagmask = flags.getMask(args);

	let entrycount = entries.len();
	if flagmask & flags.R { sort.setRev(); }
	for let i: u64 = 0; i < entrycount; ++i {
		if ls.run(entries[i], flagmask, entrycount, ws) { continue; }
		io.println("Failed to run ls on entry: ", entries[i]);
		if err.present() {
			io.println("Error: ", err.pop());
		}
		return 1;
	}
	return 0;
};