let c = @import("std/c");
let io = @import("std/io");
let vec = @import("std/vec");
let string = @import("std/string");

let stat = c.stat;

let StatInfo = struct {
	name: string.String;
	ext: string.String;
	size: string.String;
	user: string.String;
	group: string.String;
	width: i32;
	namelen: i32;
	st: stat.Stat;
	linkdead: i1;
	linkjumps: i32;
	linkloc: string.String;
	linkst: stat.Stat;
};

let newStatInfo = fn(): StatInfo {
	return StatInfo{string.new(), string.new(),
			string.new(), string.new(),
			string.new(), 0, 0,
			stat.new(), false, 0,
			string.new(), stat.new()};
};

let deinit in StatInfo = fn() {
	self.name.deinit();
	self.ext.deinit();
	self.size.deinit();
	self.user.deinit();
	self.group.deinit();
	self.st.deinit();
	self.linkloc.deinit();
	self.linkst.deinit();
};

let clear in StatInfo = fn() {
	self.name.clear();
	self.ext.clear();
	self.size.clear();
	self.user.clear();
	self.group.clear();
	self.width = 0;
	self.namelen = 0;
	self.st.clear();
	self.linkloc.clear();
	self.linkst.clear();
};

let MaxLen = struct {
	name: i32;
	user: i32;
	group: i32;
	size: i32;
	inode: i32;
	links: i32;
};

let newMaxLen = fn(): MaxLen {
	return MaxLen{0, 0, 0, 0, 0, 0};
};

let clear in MaxLen = fn() {
	self.name = 0;
	self.user = 0;
	self.group = 0;
	self.size = 0;
	self.inode = 0;
	self.links = 0;
};

let splitFile = fn(file: *const i8, name: &string.String, ext: &string.String) {
	name.clear();
	ext.clear();
	let found_dot = -1;
	let filelen = c.strlen(file);
	if filelen <= 0 { return; }

	for let i = filelen; i >= 0; --i {
		if file[i] == '.' {
			found_dot = i;
			break;
		}
	}
	name = file;
	if found_dot >= 0 {
		ext.appendCStr(&file[found_dot + 1], 0);
	}
};

let extraSpaceCount = fn(str: *const i8, usedbytes: i32): u8 {
	// 1 shift for chinese characters instead of 2 because they
	// already take space of 2 english characters and are of 3 bytes each
	let val: i64 = 0;
	for let i = 0; i < usedbytes; ++i {
		val <<= 8;
		val |= @as(u8, str[i]);
	}

	// Latin-1 Supplement are treated to be full width for some reason even
	// though they use half width only:
	// 00A0 - 00FF
	if val >= 0xC2A0 && val <= 0xC3BF { return 1; }
	// Same for Greek and Coptic set:
	// 0370 - 03FF
	if val >= 0xCDB0 && val <= 0xCFBF { return 1; }
	// 3099 - 309C are exceptions to the full width characters in Hiragana
	// These seem like they don't take any space for themselves and use the cells
	// of the previous characters
	if val >= 0xE38299 && val <= 0xE3829C { return 3; }
	// CJK Radicals Supplement, Kangxi Radicals, Ideographic Description Characters, CJK Symbols and Punctuation, Hiragana, Katakana,
	// Bopomofo, Hangul Compatibility Jamo, Kanbun, Bopomofo Extended, Katakana Phonetic Extensions, Enclosed CJK Letters and Months,
	// CJK Compatibility, CJK Unified Ideogprahs Extension A, Yijing Hexagram Symbols, CJK Unified Ideographs, Yi Syllables, Yi Radicals:
	// 2E80 - A4CF
	if val >= 0xE2BA80 && val <= 0xEA938F { return 1; }
	// Hangul: AC00 - D7AF
	if val >= 0xEAB080 && val <= 0xED9EAF { return 1; }
	// High Surrogates, High Private Use Surrogates, Low Surrogates, Private Use Area,
	// CJK Compatibility Ideographs, Alphabetic Presentation Forms:
	// D800 - FDFF
	if val >= 0xEDA080 && val <= 0xEFB7BF { return 1; }
	// CJK Compatibility, Small form variants:
	// FE30 - FE6F
	if val >= 0xEFB8B0 && val <= 0xEFB9AF { return 1; }

	// Half and / or full width characters:
	// FF00 - FFEF
	if val >= 0xEFBC80 && val <= 0xEFBFAF {
		// Halfwidth: FF65 - FF9F and FFA0 - FFDC
		if (val >= 0xEFBDA5 && val <= 0xEFBE9F) || (val >= 0xEFBEA0 && val <= 0xEFBF9C) {
			return 2;
		}
		// Full width
		return 1;
	}

	return 2;
};

let getExtraSpaces = fn(str: *const i8): u64 {
	let extraspaces = 0;
	let moveahead = 0;

	while *str {
		let c: u8 = *str;
		if c >= 0 && c <= 127 { moveahead = 1; }
		elif (c & 0xE0) == 0xC0 { moveahead = 2; }
		elif (c & 0xF0) == 0xE0 { moveahead = 3; }
		elif (c & 0xF8) == 0xF0 { moveahead = 4; }
		if moveahead > 1 {
			extraspaces += extraSpaceCount(str, moveahead);
		}
		while moveahead-- { str = @as(u64, str) + 1; }
	}

	return extraspaces;
};

// The code below is courtesy of:
//   http://www.daemonology.net/blog/2008-06-05-faster-utf8-strlen.html
//

let static comptime ONEMASK: const u64 = c.u64max / 255;

let utf8Strlen = fn(_s: *const i8): u64 {
	let s: *const i8;
	let count: u64 = 0;
	let u: u64;
	let b: u8;

	let comptime u64sz = @sizeOf(u64);

	/* Handle any initial misaligned bytes. */
	for s = _s; @as(u64, s) & (u64sz - 1); s = @as(u64, s) + 1 {
		b = *s;
		if b == 0 { return (@as(u64, s) - @as(u64, _s)) - count; }
		count += (b >> 7) & ((~b) >> 6);
	}

	/* Handle complete blocks. */
	for ; @as(u64, s) != 0; s = @as(u64, s) + u64sz {
		/* Grab 4 or 8 bytes of UTF-8 data. */
		u = *@as(@ptr(u64), s);
		/* Exit the loop if there are any zero bytes. */
		if (u - ONEMASK) & (~u) & (ONEMASK * 0x80) { break; }
		/* Count bytes which are NOT the first byte of a character. */
		u = ((u & (ONEMASK * 0x80)) >> 7) & ((~u) >> 6);
		count += (u * ONEMASK) >> ((u64sz - 1) * 8);
		let nope = false;
		// Check if string ends before or at u64sz
		for let i = 1; i <= u64sz; ++i {
			if s[i] == 0 { nope = true; break; }
		}
		if nope { break; }
	}

	/* Take care of any left-over bytes. */
	for ; ; s = @as(u64, s) + 1 {
		b = *s;
		/* Exit if we hit a zero byte. */
		if b == 0 { break; }
		/* Is this byte NOT the first byte of a character? */
		count += (b >> 7) & ((~b) >> 6);
	}
	return (@as(u64, s) - @as(u64, _s)) - count;
};
