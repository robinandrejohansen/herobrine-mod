package com.bloomlet.herobrine.entity;

import com.bloomlet.herobrine.HerobrineMod;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/**
 * WHAT SHE SAYS, AND WHY SHE IS THE ONE SAYING IT.
 *
 * The story is written down and that turned out to be the problem. Twenty-two
 * books, six of them Steve's, found one at a time, hours apart, between two
 * other things the player was doing — and every one of them has to be stopped
 * for, opened and read. The playthrough note was that the story was too hard to
 * follow, and the honest diagnosis is not that the writing was bad. It is that
 * READING IS A COST and the mod was charging it twenty-two times.
 *
 * She is the same story with the cost removed. Nothing here is new information:
 * every line below is something a book already says. What she does is say it
 * ALOUD, IN THE PLACE IT HAPPENED, AT THE MOMENT IT MATTERS — Pip's name on the
 * tower deck rather than on page two of a chest in a cellar.
 *
 * SHE IS NOT A NARRATOR AND SHE MUST NEVER SOUND LIKE ONE. Everything she says
 * is either about somebody she knew or about what to do in the next ten seconds.
 * She has no opinions about the plot, she cannot see the phase, and she never
 * once tells the player what to feel.
 *
 * RED, NOT GREY. His whispers in TheHunt are §8§o — dim, italic, unattributed,
 * deliberately hard to be sure you read. Hers are the opposite in every respect,
 * because the one thing worse than missing a line of hers is mistaking it for
 * one of his.
 *
 * AND SHE SHUTS UP. QUIET_FOR is the whole difference between a companion and a
 * talking hat: a pool can be armed and correct and still be wrong to fire,
 * because she said something ninety seconds ago.
 */
public final class Sayings {
	private Sayings() {}

	/** Nothing at all for this long after anything she said. */
	private static final long QUIET_FOR = 400L;

	// ---- WHAT SHE KNOWS ----------------------------------------------------

	static final String[] JOINING = {
		"I'm coming with you. There's nothing down here for me.",
		"My daughter opened the fourth door. She was six. I'll come.",
		"Take me with you. I can't sit in this well any longer.",
	};

	static final String[] NUDGED = {
		"I'm here.",
		"Still with you.",
		"Right behind you.",
	};

	static final String[] FALTERING = {
		"I can't — I have to get back, I'm sorry —",
		"That's too much, that's too much, I'm going —",
		"Finish it without me. Please.",
	};

	static final String[] BACK_UP = {
		"All right. I'm all right.",
		"I've eaten. Let's go.",
		"Don't look at me like that. Walk.",
	};

	public static final String[] GAUNT_SEEN = {
		"Don't. That was somebody. Do you understand that?",
		"It won't move while you're looking. So look at it.",
		"Corin. That one's called Corin, or it was.",
	};

	public static final String[] DARK = {
		"Put the torch out. Light is how he finds the room.",
		"I'd rather be in the dark than be found in the light.",
		"Forty of them had candles. Every one they had left.",
	};

	public static final String[] HIS_WORLD = {
		"This is his. Everything here is his.",
		"Whatever's in the chests here came off somebody who came first.",
		"I've heard about this place. Nobody who described it came back.",
	};

	public static final String[] YOU_DIED = {
		"I'll wait here. Come back for your things.",
		"I'm staying where you fell. Come and find me.",
		"Go on. I'll be here. I'm not moving.",
	};

	public static final String[] WALKED_TO_YOU = {
		"I waited. Then I walked.",
		"You didn't come back, so I came to you.",
		"Took me a while. I'm here now.",
	};

	// ---- SAYING IT --------------------------------------------------------

	/**
	 * One line, to one player, and not if she has just spoken.
	 *
	 * The gate is on HER rather than on the pool, which is the important half: six
	 * separate triggers each politely rate-limiting themselves still adds up to
	 * six lines at once when the player walks into a dark cave with a Gaunt in it.
	 */
	/**
	 * The same thing, reachable from outside the entity package.
	 *
	 * Company lives in manifest/ because it needs the death event and a single
	 * server-wide sweep, and the pools plus the quiet timer belong here with her.
	 */
	public static void toldOf(ServerLevel here, CompanionEntity her, Player to,
	                          String[] pool) {
		say(here, her, to, pool);
	}

	static void say(ServerLevel here, CompanionEntity her, Player to, String[] pool) {
		if (!(to instanceof ServerPlayer heard)) {
			return;
		}
		long now = here.getGameTime();
		if (now - her.lastSpoke < QUIET_FOR) {
			return;
		}
		her.lastSpoke = now;
		String line = pool[here.getRandom().nextInt(pool.length)];
		heard.sendSystemMessage(Component.literal("§c" + her.getName().getString()
			+ "§7: " + line));
		// NO SOUND. It was VILLAGER_AMBIENT — the hum — left over from when he was
		// Vera in a red coat, and it made a man with a name and a diamond sword go
		// "hrmm" like a trader every time you spoke to him. There is no vanilla
		// sound for a man saying something; the line in the chat is his voice.
		HerobrineMod.LOGGER.info("addexio to {}: \"{}\"", heard.getName().getString(), line);
	}

	/**
	 * THE FIRST THING HE SAYS, AND IT IS SEVERAL THINGS.
	 *
	 * He walked sixty blocks to the first house and then stood there until somebody
	 * clicked him, and the only thing he had to say was one line from a pool. A
	 * companion the whole story is narrated by should introduce himself: who he
	 * is, why he is here, what he wrote, and what he is for. Five lines, three and
	 * a half seconds apart, in the order they are written, past the quiet timer.
	 * Once per world — see CompanionEntity.INTRODUCED.
	 */
	/**
	 * HIS LINE, WITH HIS NAME ON IT.
	 *
	 * Everything he said arrived as dark grey italics and nothing else — which in a
	 * chat where your own messages are the only other thing reads as something YOU
	 * typed. Addexio has always had his name in front. So does he now, in the white
	 * that is the only colour he has; the words stay grey and in italics, because
	 * they are still a whisper, only now you know whose.
	 */
	public static Component his(String line) {
		return Component.literal("§fHerobrine§7: §8§o" + line);
	}

	/** What he says when Herobrine puts him down. Once. */
	public static final String[] FALLEN = {
		"It's all right. Finish it.",
		"Go. Don't look at me. Go.",
		"I said I'd hold what was behind you. I held it.",
		"Not this time either. Finish it for me.",
	};

	/**
	 * THE INTRODUCTION IS A WARNING WITH A NAME ON IT. The first version said who he
	 * was and stopped there. This one says why there is no time: what he has seen,
	 * what happened to everyone else, and that the map in your hand is his. Easy
	 * words, one thought a line, and the pace of a man who keeps looking at the
	 * treeline while he talks.
	 */
	static final String[] INTRODUCTION = {
		"Don't stop here. Not at this house. He knows this house.",
		"Addexio. I lived here, before. I wrote the ten books you will find — one in every place he took.",
		"He has been seen again. Three nights ago. He does not come back for nothing.",
		"The map in your hand — I sent it. It goes to the town. Follow it, and do not read it out loud.",
		"Everyone I have ever walked this road with is dead. I am telling you now, while you can still say no.",
		"You're still here. Good. Then we move before dark.",
		"Walk. I'll keep up. And if you hear me stop talking — run.",
	};

	/**
	 * WHAT HE SAYS WHEN A PLACE IS FOUND. One story per place, in the order the
	 * books tell it, and each one is his: something he saw, something he lost, or
	 * something that is about to happen. They do the work the books do, out loud,
	 * for the player who has not opened the chest yet.
	 */
	public static final String[] FOUND_TOWN = {
		"The town. Don't go down the well first. I'll tell you why when we're out.",
		"He got into people here. Not their houses — them. The baker held his own daughter under the water until she stopped. He was smiling. It was not his smile.",
		"They went underground to get away from the sky. Some of them are still down there. Do not answer if they call your name.",
	};
	public static final String[] FOUND_TOWER = {
		"His tower. He built it to look down on us. I stood where you are standing and watched the top light up every night for a month.",
		"I went up once. There was a table with straps on it. The straps were the right size for a child.",
		"We are being watched. Don't turn round. Just walk.",
	};
	public static final String[] FOUND_GAOL = {
		"The prison. Cell nine. They kept a man in there for eleven weeks and cut something out of him every Sunday.",
		"He came back. The only one who ever did. He could not stop laughing, and he could not tell us why.",
		"If you hear something breathing behind a door down there, it is not asleep. It is counting.",
	};
	public static final String[] FOUND_CHURCH = {
		"This is where he lived, between the times he was seen. Under the altar. In the dark, with the bells.",
		"The priest wrote it all down, then nailed his own hands to the door so he could not write any more. It is still in the book.",
		"We are close now. He knows. He always knows.",
	};
	public static final String[] FOUND_THRESHOLD = {
		"The last house. This is where they lost him — and where I stopped looking, because I was afraid of what finding him would mean.",
		"The door under this house goes to his side. Everyone who went through and came back, came back wrong.",
		"I am going through with you this time. Say nothing. Walk.",
	};

	/** A short story, one line at a time, at reading pace, past the quiet timer. */
	public static void tell(ServerLevel here, CompanionEntity her, ServerPlayer to, String[] lines,
	                        int after) {
		int at = after;
		for (String line : lines) {
			final String said = line;
			com.bloomlet.herobrine.manifest.Cadence.in(here.getServer(), at, () -> {
				if (her.isAlive() && !her.isFallen() && to.isAlive()) {
					to.sendSystemMessage(Component.literal("§c" + her.getName().getString()
						+ "§7: " + said));
					her.lastSpoke = here.getGameTime();
				}
			});
			at += beatFor(line);
		}
	}

	/**
	 * HOW LONG A LINE STAYS BEFORE THE NEXT: as long as it takes to read it.
	 *
	 * Every line got three and a half seconds, and the longest was twenty words —
	 * which is a fast reader's pace with nothing else going on, and there is a game
	 * going on. A second and a half to notice a line has appeared, then 0.45 s a
	 * word (about 130 words a minute, a comfortable reading pace, not a skimming
	 * one), never under three seconds. The two long lines were also split, so no
	 * single line carries more than one thought:
	 *
	 *     10 words  6.0 s      15 words  8.3 s      16 words  8.7 s
	 *
	 * The whole introduction is about forty-five seconds. It is said once per world.
	 */
	static int beatFor(String line) {
		int words = 0;
		for (String token : line.split(" ")) {
			if (token.chars().anyMatch(Character::isLetter)) {
				words++;
			}
		}
		return Math.max(60, 30 + 9 * words);
	}

	static int introductionLength() {
		int total = 0;
		for (String line : INTRODUCTION) {
			total += beatFor(line);
		}
		return total;
	}

	static void introduce(ServerLevel here, CompanionEntity her, ServerPlayer to) {
		int at = 0;
		for (String line : INTRODUCTION) {
			final String said = line;
			com.bloomlet.herobrine.manifest.Cadence.in(here.getServer(), at, () -> {
				if (her.isAlive() && to.isAlive()) {
					to.sendSystemMessage(Component.literal("§c" + her.getName().getString()
						+ "§7: " + said));
					her.lastSpoke = here.getGameTime();
				}
			});
			at += beatFor(line);
		}
		HerobrineMod.LOGGER.info("addexio introduces himself to {} — {} lines, {} seconds",
			to.getName().getString(), INTRODUCTION.length, at / 20);
	}

	/**
	 * Whether a thing in the world is one of his.
	 *
	 * Used by Falter to decide what to run from, so it has to be the mod's own
	 * roster and not "anything hostile" — a cave spider is not what she is afraid
	 * of, and a companion who bolts from a zombie is a companion who is always
	 * bolting.
	 */
	static boolean isHis(Mob what) {
		return what instanceof HerobrineEntity
			|| what instanceof GauntEntity
			|| what instanceof TurnedEntity
			|| what instanceof MimicEntity
			|| what instanceof InfectedEntity;
	}
}
