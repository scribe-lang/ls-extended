let builder = @import("std/builder");

let setup in builder.Builder = fn(): i1 {
	self.addExe("ls-extended", "src/main.sc");
	return true;
};
