let builder = @import("std/builder");

let setup in builder.Builder = fn(): i1 {
	self.addExe(ref"ls-extended", ref"src/main.sc");
	return true;
};
