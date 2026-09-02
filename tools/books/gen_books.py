import sys, re
S=None
sys.path.insert(0, __import__('os').path.dirname(__import__('os').path.abspath(__file__)))
from bind import bind
from prose import HOUSEBOOKS

DOC = {
 "one": "1. THE FARM. Who he is, who Herobrine is, and the first small wrong things —\n\t * torches out in a line, a door open and still, prints going one way. Ends by\n\t * sending you down the passage under the outbuilding, which is where book 2 is.",
 "two": "2. WHAT I SAW. The thing standing in his wheat that does not breathe, and the\n\t * only chapter where he does something right: he boards his neighbours' windows\n\t * and carries Marek's children to the town. Then the rider arrives.",
 "three": "3. THE TOWN. Ashfold on the twelfth day — every dog facing one way, nine people\n\t * wearing other people's faces, and the tall one walking down the main street\n\t * with its mouth open. Bren shoots it twice and it keeps walking.",
 "four": "4. WHAT HE DID HERE. The worst book in the mod and the one the rest hangs off.\n\t *\n\t * The door in the square, forty-one people walked through it, and then the\n\t * church roof and Marek's four children. Addexio was thirty feet away with an\n\t * iron sword and stayed behind the cart. He says so in one sentence and does\n\t * not soften it, which is the whole character in one line.",
 "five": "5. THE TOWER. Nine years of watching the wrong direction. Pip comes up the\n\t * ladder shaking, nobody believes him, four go down and two come back — and\n\t * what is under the tower turns out to be ours, not his.",
 "six": "6. THE PRISON. What the gaol was for, and Joren's ledger with two columns where\n\t * the out column is shorter.\n\t *\n\t * Also the only place in the mod that tells you how to beat the mimic, and\n\t * every tell is real: it closes the last paces itself, copies your crouch,\n\t * ransacks containers without taking anything, changes its coat, and only ever\n\t * does any of it while you are alone.",
 "seven": "7. THE ONE WHO CAME BACK. A survivor's account, transcribed and untidied.\n\t *\n\t * Eleven weeks in cell nine, and the men doing it were the town's own. The\n\t * point is stated by the man it was done to: the ordinary ones had to be cut\n\t * too, or the numbers mean nothing. The man with the book was Steve, and he\n\t * was only ever testing himself.",
 "eight": "8. WHERE HE LIVES. Two boys and a dog find the second door. The church is where\n\t * two hundred people decided whether to walk into it, and where Wendel wrote\n\t * down the question nobody would let him ask out loud. Four of nine came back,\n\t * and what they saw was a paved road and lit windows: he built a house.",
 "nine": "9. THE LAST HOUSE. The one fight Addexio is not ashamed of, and it lasts a\n\t * sentence. He reaches Herobrine, swings, connects, and wakes against the far\n\t * wall with a hand that never works again. Sixty-one dead in one room. They\n\t * wall the door up and tell the valley it is over.",
 "ten": "10. HE HAS BEEN SEEN. Written last spring, at the bottom of the stair, next to\n\t * the hole in his own wall.\n\t *\n\t * The list of what he got wrong is the mod's own advice in order, and the last\n\t * page is why the numbering exists at all: he is still alive, he refuses to say\n\t * where he is because you already know, and he has been walking behind you\n\t * since the first house. He came down here alone to leave this book at the\n\t * door and then went back up to the farm to wait. See Company.arrives.",
}

def block(pages):
    out = []
    for p in pages:
        # EVERY DOUBLE QUOTE ESCAPED, and it does not show up until somebody
        # writes dialogue. A page is emitted with the closing delimiter hard
        # against the last character, so a page ENDING in a quote makes four in
        # a row and javac reads a closed block followed by an unclosed string.
        # Book 7 is a transcript and every paragraph in it ends with a quote:
        # seven pages broke at once. In a text block \" is exactly a quote, so
        # escaping all of them is free and cannot be got wrong later.
        lines = p.replace('"', '\\"').split("\n")
        body = "\n".join(("\t\t\t" + l) if l else "" for l in lines)
        out.append('\t\t\t"""\n' + body + '"""')
    return ",\n\n".join(out)

SECTIONS = [
 (1, "THE FARM", ["one","two"]),
 (2, "THE TOWN", ["three","four"]),
 (3, "THE TOWER", ["five"]),
 (4, "THE GAOL", ["six","seven"]),
 (5, "THE CHURCH", ["eight"]),
 (6, "THE LAST HOUSE", ["nine","ten"]),
]
BY = dict(HOUSEBOOKS)

parts = []
for n, label, names in SECTIONS:
    parts.append("\t// ---- %d. %s %s\n" % (n, label, "-" * max(1, 58 - len(label))))
    for name in names:
        title, author, prose = BY[name]
        pages = bind(prose)
        for p in pages:
            assert p.count("\n") + 1 <= 14, (name, p)
            assert '\\' not in p, name      # escaping is block()'s job
        parts.append('\t/**\n\t * %s\n\t */\n\tpublic static ItemStack %s() {\n\t\treturn book("%s", "%s",\n%s);\n\t}\n'
                     % (DOC[name], name, title, author, block(pages)))

HEAD = '''/**
 * THE STORY. TEN BOOKS, NUMBERED, ONE VOICE, IN ORDER.
 *
 * GENERATED. Do not edit this file — write tools/books/prose.py and run
 * tools/books/gen_books.py. Pagination is the reason: a page is about 114 pixels
 * wide and fourteen rows tall and overflow is SILENT, so a hand-edited paragraph
 * loses its last line and nobody finds out for months. Seven pages of the first
 * set were clipped exactly that way.
 *
 * WHO IS TELLING IT. Addexio. He lived in the first house, he was at every one
 * of these places while it happened, and he is the companion you can still find
 * who walks in out of the distance at that same first house — see
 * Company.arrives. Book 10 is addressed to you and its last page is the man
 * already standing next to you, which is the whole reason the books are
 * numbered rather than scattered.
 *
 * HE IS A HERO WHO FAILED. He had a weapon every time. Book 4 is the one the
 * rest hangs off: he was thirty feet from a church with four children in it,
 * holding an iron sword, and he stayed behind a cart. He writes it in one
 * sentence and does not soften it. The arc after that goes downward — a tower
 * watching the wrong direction, a prison his own town built, a fight he lost,
 * and a wall he helped put up over a door instead of going through it.
 *
 * WHAT REPLACED WHAT. There were twenty-two books in ten hands: Alma's house
 * book, Toby's, Joren's protocol, Kadmus on the watch, and Steve's six documents
 * laid over the top. Every one was good and the whole was unreadable — a player
 * finds these one at a time, hours apart, between two other things, and
 * assembling a plot out of ten strangers' diaries is a thing nobody does.
 *
 * Steve is still in it. He is a character in Addexio's account now instead of
 * its narrator, and book 7 is where he arrives: the man with the book who came
 * to the prison and was only ever testing himself.
 *
 * WHAT EVERY BOOK HAS TO DO, or it is scenery:
 *
 *   MAKE THIS PLACE MATTER   what happened in the room you are standing in
 *   POINT AT THE NEXT ONE    by name, with a reason to walk there
 *   MOVE THE STORY ON        so that the order is not decoration
 *
 * Book 1 is in the farmhouse chest WITH THE MAP, because page one says "There is
 * a map in this chest" — the two cannot be separated any more without making the
 * text wrong. Book 6 carries the mimic tells that used to be theProtocol, in the
 * building that was dug to answer that exact question and answered it the worst
 * possible way.
 *
 * EASY WORDS AND SHORT LINES. Nineteen characters to a line means a long word
 * costs a line and a subordinate clause costs a page, and it is read on a screen
 * by somebody who is being hunted.
 */'''

src = open("src/main/java/com/bloomlet/herobrine/structure/HouseBooks.java").read()
# The class docstring is the first /** sitting at column 0 after the imports.
# Anchoring on its TEXT made this script single-use: the second run could not
# find a header it had itself replaced.
top = src[:src.index("\n/**") + 1]
tail = src[src.index("\tprivate static final int TITLE_FITS"):]
new = top + HEAD + "\npublic final class HouseBooks {\n\tprivate HouseBooks() {}\n\n" + "\n".join(parts) + "\n" + tail
open("src/main/java/com/bloomlet/herobrine/structure/HouseBooks.java","w").write(new)
print("wrote HouseBooks.java  (%d books)" % len(BY))

# ══════════════════════════════════════════════ LabBooks
from prose import LABBOOKS
LDOC = {
 "intake": "THE REGISTER, and it is deliberately the driest thing in the mod.\n\t *\n\t * A numbered list of neighbours by their trade does something no description\n\t * could: it shows the exact moment they stopped being people and became\n\t * entries, and it does it in the FORMAT rather than in the words. The worst\n\t * line in it is a man noticing he has started writing \"no change\" as though it\n\t * were a disappointment.\n\t *\n\t * Subject three is Wendel, cleric. Wendel is also the author of\n\t * HouseBooks.eight, four sites and some years later — the man who tells you\n\t * to ask Steve what he did to Herobrine before you let him near you. He knows\n\t * because he was in one of these cells.",
 "theDoor": "WHAT THE THING IN THE NEXT ROOM ACTUALLY IS, measured in a farm ledger's\n\t * vocabulary because that is the only vocabulary he has.\n\t *\n\t * No ritual, no incantation, no glowing anything. A man notes that it opens for\n\t * one person and not for him, builds a room around it to test that, and records\n\t * the result in one word. \"It can.\"",
 "subjectNine": "SUBJECT NINE IS WHERE THE GAUNTS COME FROM.\n\t *\n\t * Taller than he was, does not eat, does not sleep, does not blink, waits at\n\t * the bars for somebody to be curious — that is GauntEntity, described from the\n\t * outside by the man who made it. It is not a monster the mod happens to have;\n\t * it is a person from the mill road called Corin.\n\t *\n\t * The last line is the one that matters: he has written that sentence before,\n\t * years ago, about his brother.",
 "lastDay": "AND SEVEN OF THEM WALKED OUT.\n\t *\n\t * The bars are out, not in. This is the book that puts every tall silent thing\n\t * in the forest into the story by name — Aldous, Hesk, Mila, Bo, Ren, Sera and\n\t * a smith's boy of fifteen — and hands the player the sentence the whole mod\n\t * needed: they are mine, I made them out of my neighbours, and they are still\n\t * out there waiting for somebody to be curious.\n\t *\n\t * Wendel is the only one who stayed. He does not say why and he did not ask.",
 "whatIWas": "PLAINLY, ONCE.\n\t *\n\t * The confession as he wrote it AT THE TIME, years before the six documents. He\n\t * did not do it to save his friend. He did it to find out whether it could have\n\t * been him, and the answer was yes.\n\t *\n\t * Then the count, which is the point of the whole file: nine names, and three\n\t * he had stopped writing down. Three graves at the farm and none of them are\n\t * theirs.",
}
lp = []
for name, (title, author, prose) in LABBOOKS:
    pages = bind(prose)
    for p in pages:
        assert p.count("\n") + 1 <= 14 and '"""' not in p and "\\" not in p, name
    lp.append('\t/**\n\t * %s\n\t */\n\tpublic static ItemStack %s() {\n\t\treturn book("%s", "%s",\n%s);\n\t}\n'
              % (LDOC[name], name, title, author, block(pages)))

LHEAD = '''/**
 * STEVE'S OWN NOTEBOOK, FROM THE TIME.
 *
 * HouseBooks.seven is Addexio naming him years afterwards, with the
 * headings gone and the worst sentence written on purpose. These are the notes
 * he was actually keeping WHILE HE DID IT, and the gap between the two registers
 * is the horror of the room: the retrospective one says "I made them out of my
 * neighbours". The contemporaneous one says "no change".
 *
 * A player finds both in the same building, feet apart. That is deliberate.
 *
 * NOTHING SUPERNATURAL IS EVER DESCRIBED IN HERE, and this is the one file where
 * that rule still earns its keep. No rituals, no incantations. A man measures a
 * thing he does not understand in the only vocabulary he has, which is a farm
 * ledger, and that is what makes it read like somebody's actual notebook.
 *
 * WHAT THIS FILE IS LOAD-BEARING FOR. Two things nothing else in the mod
 * explains:
 *
 *   GauntEntity. Subject nine is Corin, from the mill road. Taller than he was,
 *   no eating, no sleeping, no blinking, stands at the bars waiting for somebody
 *   to be curious. lastDay names the seven who walked out with him. Every tall
 *   pale thing in the forest is a neighbour of Steve's with a name.
 *
 *   Wendel. Subject three, the cleric who would not stop talking, is the same
 *   Wendel, whom HouseBooks.eight quotes at the church years later, telling
 *   the player to ask Steve what he did to Herobrine. He is the only subject who
 *   stayed. Steve does not say why and did not ask.
 */'''

lsrc = open("src/main/java/com/bloomlet/herobrine/structure/LabBooks.java").read()
ltop = lsrc[:lsrc.index("\n/**") + 1]
ltail = '''	private static final int TITLE_FITS = 32;

	/**
	 * The wire caps a book title at 32 characters and THROWS rather than truncating.
	 *
	 * HouseBooks has carried this guard since a 41-character title disconnected the
	 * player who opened the chest — an EncoderException out of Utf8String, which
	 * looks like a network fault and not like a typo in a book. This file had no
	 * guard at all and passed the title straight through.
	 */
	private static String title(String wanted) {
		if (wanted.length() <= TITLE_FITS) {
			return wanted;
		}
		String short_ = wanted.substring(0, TITLE_FITS).trim();
		com.bloomlet.herobrine.HerobrineMod.LOGGER.warn(
			"book title was {} characters and the wire allows {} — \\"{}\\" became \\"{}\\"",
			wanted.length(), TITLE_FITS, wanted, short_);
		return short_;
	}

	private static ItemStack book(String title, String author, String... pages) {
		List<Filterable<Component>> written = new ArrayList<>();
		for (String page : pages) {
			written.add(Filterable.passThrough(Component.literal(page)));
		}
		ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
		stack.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
			Filterable.passThrough(title(title)), author, 0, written, true));
		return stack;
	}
}
'''
open("src/main/java/com/bloomlet/herobrine/structure/LabBooks.java","w").write(
    ltop + LHEAD + "\npublic final class LabBooks {\n\tprivate LabBooks() {}\n\n"
    + "\n".join(lp) + "\n" + ltail)
print("wrote LabBooks.java  (%d books)" % len(LABBOOKS))
