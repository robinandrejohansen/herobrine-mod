import sys, re
S=None
sys.path.insert(0, __import__('os').path.dirname(__import__('os').path.abspath(__file__)))
from bind import bind
from prose import HOUSEBOOKS

DOC = {
 "household": "Alma. She is the one who decides they are leaving, and she is the first name in the mod.",
 "child": "Toby, nine. The only book here written by somebody who is not frightened of him yet.",
 "ledger": "STEVE'S OWN LEDGER, LEFT ON THE KITCHEN SHELF, and it is the introduction the\n\t * mod did not have.\n\t *\n\t * Two hands in one house: a man who was happy here four years, and the family\n\t * who moved in after and found his handwriting. It names Herobrine on the first\n\t * page, says plainly that they were friends, and says what happened under the\n\t * hill. Everything else in all six chapters is downstream of this book.",
 "tally": "Nils, counting. The only book that is mostly a number.",
 "farRoom": "Alma again, four nights later. The point where the family stops being a family\n\t * with a problem and becomes a family that is leaving.",
 "brother": "Nils, in the cellar under his own kitchen. The wearing-a-face idea, planted at\n\t * the first house so that the gaol at site four is a confirmation and not a\n\t * surprise.",
 "theTown": "Ashfold, in its own hand. Written from the bottom of the well they now live in.",
 "buried": "Kadmus of the watch. Three men, and the one who does not come down is the boy.",
 "theDig": "Joren the warder — and the book that first makes Steve suspicious.\n\t *\n\t * Joren wrote to Steve asking how to tell them apart and got a correct answer\n\t * back inside a week. He notes that he did not think about that at the time.\n\t * A player who is paying attention gets the whole of site six two houses early,\n\t * and a player who is not loses nothing.",
 "theShrine": "Wendel the cleric, and the last line is the mod's best warning.\n\t *\n\t * He tells you to ask Steve what he did to Herobrine before letting him near\n\t * you — written by a man who does not know he is already dead, in a building\n\t * Steve was four days away from and chose not to reach.",
 "theHomesteadAfter": "SITE ONE OF STEVE'S SIX, and the shape every one of them keeps.\n\t *\n\t * WHAT HE IS / WHO LIVED HERE / WHAT HE DID / WHAT I GOT WRONG / WHERE TO GO.\n\t * Same five headings, same order, six times. Learn the form once and you know\n\t * where to look in all of them, which is the entire point — the old books were\n\t * six unrelated diaries and a player had to assemble the plot from the gaps.\n\t *\n\t * It also explains the map chain. \"There is a map in this chest. I drew it\n\t * myself.\" leaveTheWay has always put the map in the same container as the book;\n\t * this is the first time the fiction accounts for why.",
 "theTownAfter": "SITE TWO. Where the lying starts.\n\t *\n\t * Steve came to Ashfold three weeks before it happened and told four hundred\n\t * people Herobrine was dead. Eleven of them died with their shutters barred and\n\t * no reason on earth to be afraid. He says it is on him and not on the wall.",
 "theTowerAfter": "SITE THREE. Where it stops being a lie of omission.\n\t *\n\t * He helped them site this tower knowing that watching is not the problem, and\n\t * let three men spend a summer on it rather than say so. Pip was seventeen.",
 "theProtocol": "HOW TO TELL HIM FROM A PERSON, which is the one thing this building knows that\n\t * nothing else in the mod tells you.\n\t *\n\t * Every tell is real, read off MimicEntity.TheFriend in the order that goal\n\t * runs them: COMES_TO 6, the crouch toggle on GREET_EVERY, ransack,\n\t * DRESSES_EVERY, STRIKES 3, then BOLTS at twice a sprint. The last page is the\n\t * counter — the goal will not start unless a player is SEPARATED — so \"do not\n\t * be alone down here\" is a mechanic stated plainly rather than atmosphere.\n\t *\n\t * In Joren's hand, copied from a letter. Site four says who wrote the letter\n\t * and how he knew the answer, and that is the worse half of it.",
 "theGaolAfter": "SITE FOUR. Where he admits he has done this before.\n\t *\n\t * The protocol in the same chest is correct because Steve had already run it,\n\t * years earlier, on people who had no idea why they were in a cell. He did not\n\t * mention that in his reply. Joren died thinking he was clever.",
 "theChurchAfter": "SITE FIVE. Where he runs out of excuses one book early.\n\t *\n\t * He was four days away, he knew which night it was, and he did not come. He\n\t * writes down that he was afraid, says that is true and is not the reason, and\n\t * admits he has been putting the reason off for five of these.",
 "theThresholdAfter": "SITE SIX, AND THE HEADINGS ARE GONE.\n\t *\n\t * The form has been degrading for two books and here it stops pretending. No\n\t * WHO LIVED HERE, no WHERE TO GO until the end, just a man writing the thing\n\t * he has avoided writing five times.\n\t *\n\t * He did not experiment on eleven people to save his friend. He did it to find\n\t * out whether he himself would have come up out of that hole the same way, and\n\t * the answer was yes. Then he used eight people as bait, gave the order, sealed\n\t * the door and told a walled town it was over.\n\t *\n\t * HE IS NOT DEAD used to be clipped off the end of a page. It is on its own\n\t * page now.",
}

def block(pages):
    out = []
    for p in pages:
        lines = p.split("\n")
        body = "\n".join(("\t\t\t" + l) if l else "" for l in lines)
        out.append('\t\t\t"""\n' + body + '"""')
    return ",\n\n".join(out)

SECTIONS = [
 (1, "THE FARM", ["ledger","household","child","tally","farRoom","brother","theHomesteadAfter"]),
 (2, "THE TOWN", ["theTown","theTownAfter"]),
 (3, "THE TOWER", ["buried","theTowerAfter"]),
 (4, "THE GAOL", ["theDig","theProtocol","theGaolAfter"]),
 (5, "THE CHURCH", ["theShrine","theChurchAfter"]),
 (6, "THE SEAM", ["theThresholdAfter"]),
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
            assert '"""' not in p and "\\" not in p, name
        parts.append('\t/**\n\t * %s\n\t */\n\tpublic static ItemStack %s() {\n\t\treturn book("%s", "%s",\n%s);\n\t}\n'
                     % (DOC[name], name, title, author, block(pages)))

HEAD = '''/**
 * THE STORY, AND WHO IS TELLING IT.
 *
 * Two kinds of book, and the difference is the whole design.
 *
 * STEVE'S SIX. One at each site, left on purpose, for whoever comes after him.
 * Same five headings every time — what he is, who lived here, what he did, what
 * I got wrong, where to go. A single voice a player learns to read once, that
 * knows more than it is saying, and that is pointing at the next building and
 * explaining why. These are theHomesteadAfter through theThresholdAfter.
 *
 * AND WHAT THE DEAD WROTE. Alma's house book, Toby's book, Joren's protocol,
 * Kadmus on the watch. The primary sources Steve is writing up, in the hands of
 * the people it happened to, found in the rooms they were written in.
 *
 * WHY IT WAS REBUILT. Feedback after the first full playthrough, and it was
 * blunt: too hard, no introduction, nothing you could picture, and — worst of it
 * — HEROBRINE IS NEVER NAMED. Not once in twenty-two books. He was "he", "him",
 * "HIM", for the whole mod. Steve did not exist at all. Everybody was an initial:
 * M., R., J., and an em dash.
 *
 * The old rule was NOTHING SUPERNATURAL IS EVER DESCRIBED — every entry a
 * domestic observation, the horror left for the reader to assemble. It reads
 * beautifully and it fails the job. A player finds these one at a time, hours
 * apart, between two other things; restraint nobody assembles is not restraint,
 * it is a story that did not get told.
 *
 * So: names. Alma, Nils, Toby, Otto, Marek, Kadmus, Bren, Pip, Joren, Wendel.
 * An introduction, on page one of Steve's old ledger, which says who Herobrine
 * was and what he touched. Plain words, short lines, and the worst thing said
 * out loud rather than implied.
 *
 * PAGINATION IS NOT OPTIONAL HERE. A page renders about 114 pixels wide and
 * fourteen rows tall, and overflow is SILENT — seven pages of the old set were
 * losing their last lines, including the page that says HE IS NOT DEAD. Every
 * page below was laid out against the real glyph widths before it was written
 * in. If you edit one by hand, count the rows.
 *
 * The one thing kept from the old rule is the contrast: these people use capital
 * letters and full stops right up to the moment they stop writing. He does not.
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
 "intake": "THE REGISTER, and it is deliberately the driest thing in the mod.\n\t *\n\t * A numbered list of neighbours by their trade does something no description\n\t * could: it shows the exact moment they stopped being people and became\n\t * entries, and it does it in the FORMAT rather than in the words. The worst\n\t * line in it is a man noticing he has started writing \"no change\" as though it\n\t * were a disappointment.\n\t *\n\t * Subject three is Wendel, cleric. Wendel is also the author of\n\t * HouseBooks.theShrine, four sites and some years later — the man who tells you\n\t * to ask Steve what he did to Herobrine before you let him near you. He knows\n\t * because he was in one of these cells.",
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
 * HouseBooks.theThresholdAfter is Steve confessing years afterwards, with the
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
 *   Wendel who writes HouseBooks.theShrine at the church years later and tells
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
