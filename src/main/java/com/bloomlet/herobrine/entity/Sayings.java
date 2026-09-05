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
	private static final long QUIET_FOR = 600L;      // thirty seconds. twenty made him a metronome

	// ---- WHAT SHE KNOWS ----------------------------------------------------

	static final String[] JOINING = {
		"I'm coming with you. There's nothing down here for me.",
		"My daughter opened the fourth door. She was six. I'll come.",
		"Take me with you. I can't sit in this well any longer.",
	};

	/*
	 * LEADING. He comes for you at first light and brings you to the farm. One
	 * sentence when he reaches you, a word when you fall behind, the map if you
	 * will not follow, and one word when you are there — the introduction says the
	 * rest, so this says as little as it can.
	 */
	static final String[] LEAD_FIRST = {
		"You need to see something. Come with me — and don't stop for anything.",
	};
	static final String[] LEAD_WAITING = {
		"Keep up.",
		"It's not far. Don't stop here.",
		"This way. Please.",
		"I'm not going without you.",
	};
	static final String[] LEAD_MAP = {
		"Then take this. Follow it, if you won't follow me.",
	};
	static final String[] LEAD_ARRIVED = {
		"Here.",
	};

	static final String[] NUDGED = {
		"I'm here.",
		"Still with you.",
		"Right behind you.",
		"Go on. I've got the back.",
		"What is it? Did you see something?",
		"Same road. Keep going.",
	};

	/** Frightened, not leaving. He never says he is going; he is not. */
	static final String[] SCARED = {
		"Too many — keep moving, I'm right behind you.",
		"Not like this. Not here.",
		"Get me a second. One second.",
		"Where did they all come from?",
		"Fall back. FALL BACK.",
		"I can't hold this many. Run.",
	};

	static final String[] BACK_UP = {
		"All right. I'm all right.",
		"I've eaten. Let's go.",
		"Don't look at me like that. Walk.",
		"That was close. Too close. Are you hurt?",
		"I'm up. Where were we.",
		"Nothing broken. Nothing that matters.",
	};

	public static final String[] GAUNT_SEEN = {
		"Don't. That was somebody. Do you understand that?",
		"It won't move while you're looking. So look at it.",
		"Corin. That one's called Corin, or it was.",
		"Why is it so tall? They were never that tall.",
		"Keep your eyes on it and back away. It cannot be watched and fought.",
		"It knows you're here. It knew before you did.",
		"Do you think there is anybody left in there?",
	};

	public static final String[] DARK = {
		"Put the torch out. Light is how he finds the room.",
		"I'd rather be in the dark than be found in the light.",
		"Forty of them had candles. Every one they had left.",
		"Do you hear that? No. Neither do I. That's what worries me.",
		"How long have we been down here?",
		"Something has been through here. Look at the floor.",
		"Stay close. If I go quiet, it isn't by choice.",
		"He likes it down here. I don't know why I know that.",
	};

	public static final String[] HIS_WORLD = {
		"This is his. Everything here is his.",
		"Whatever's in the chests here came off somebody who came first.",
		"I've heard about this place. Nobody who described it came back.",
		"Don't drink the water here. I don't know what it is, but it isn't water.",
		"The houses have beds. Nobody has slept in them. Nobody could.",
		"He can hear us. Not the words. The footsteps.",
		"Stay where I can see you. Please.",
		"What did he do to the sky? It hasn't moved since we came through.",
		"Do you think they knew, the ones he took? That this is where they'd end up?",
		"Every window in that city is dark. Where are they all?",
		"Which way is the castle? No — don't answer. I can feel which way.",
		"If I fall here, don't stop for me. I mean that.",
	};

	public static final String[] YOU_DIED = {
		"I'll wait here. Come back for your things.",
		"I'm staying where you fell. Come and find me.",
		"Go on. I'll be here. I'm not moving.",
		"What happened? What did that to you?",
		"I saw it. I couldn't get there in time.",
		"Your things are here. All of it. I counted.",
	};

	public static final String[] WALKED_TO_YOU = {
		"I waited. Then I walked.",
		"You didn't come back, so I came to you.",
		"Took me a while. I'm here now.",
		"Where did you go? I lost you at the trees.",
		"Don't do that again. Please.",
		"Next time say something before you run.",
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
		if (pool.length > 1 && line.equals(her.lastSaid)) {
			line = pool[(java.util.Arrays.asList(pool).indexOf(line) + 1 + here.getRandom().nextInt(pool.length - 1)) % pool.length];
		}
		her.lastSaid = line;
		voice(here, her, line);
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

	/** What he says the first time he stands on the other side of the way. Once per world. */
	public static final String[] CROSSED = {
		"So this is where he goes. I always wondered. I wish I still did.",
		"This is where they lost him — and where the ones who came back stopped being who they were. I read their faces. I never read the books they left.",
		"There is a city ahead. Do not go into the houses. And the castle past it is his, and he is in it, and he knows we are here.",
		"I am not leaving this time. Whatever it costs. Walk.",
	};

	/** A line from a pool, but no oftener than `quiet` ticks since he last spoke. */
	public static void toldOfRarely(ServerLevel here, CompanionEntity her, Player to, String[] pool,
	                                long quiet) {
		if (here.getGameTime() - her.lastSpoke < quiet) {
			return;
		}
		say(here, her, to, pool);
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
	 * what happened to everyone else, and where the next map is: up the tower. Easy
	 * words, one thought a line, and the pace of a man who keeps looking at the
	 * treeline while he talks.
	 */
	static final String[] INTRODUCTION = {
		"Don't stop here. Not at this house. He knows this house.",
		"Addexio. I lived here, before. I wrote six books — one in every place he took. Read them in order.",
		"He has been seen again. Three nights ago. He does not come back for nothing.",
		"There is a map at the top of the tower. It goes to the town. Climb up and take it — and do not read it out loud.",
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
	/** His watch, seen from the road. Told once, when it is posted and Addexio is walking with you. */
	/** Bodies on the ground where he walks. A man who notices, and asks. */
	public static final String[] CORPSES = {
		"What happened here?",
		"Who did this? All of them at once?",
		"Don't touch them. Look at the faces first.",
		"They didn't run. Why didn't they run?",
		"I knew that one. I think I knew that one.",
	};
	/** The light going, out in the open. Once a night. */
	public static final String[] NIGHTFALL = {
		"It's getting dark. He likes the dark. Stay close.",
		"Night. Walls or a hole before it's full dark, one or the other.",
		"Don't sleep tonight. I won't either.",
		"He comes further out at night. Everything does.",
	};
	/** The player low on health. */
	public static final String[] HURT = {
		"You're bleeding. Eat something. Now.",
		"Stop. You're hurt worse than you think.",
		"Sit down a minute. I'll watch.",
	};
	/** Long quiet stretches. A man thinking out loud, so you know he's still a man. */
	public static final String[] MUSING = {
		"Do you think he remembers them? The ones he took?",
		"I used to think he was one of us once. I don't think that any more.",
		"What did you do, before this? Before him?",
		"The corn was flat in a circle. I still see it when I close my eyes.",
		"If we finish this — when we finish this — I am going to sleep for a year.",
		"He never speaks. Have you noticed? Everything else does.",
		"Why us? Why did he come to my farm and not the next one?",
	};
	public static final String[] WATCHED = {
		"He's left a watch on it. Not villagers — look at how they stand. They don't fidget.",
		"They won't come past the fence. They don't have to. What you came for is inside.",
	};
	public static final String[] FOUND_TOWN = {
		"The town. He drowned the baker's girl in that trough in front of everyone, wearing the baker.",
		"Half of them are under the church. They will call you by name. Do not answer.",
		"The map is in the library down there. Take it and we leave.",
	};
	public static final String[] FOUND_TOWER = {
		"His house is under this hill. He ate at a table set for two. The other chair was my daughter's.",
		"There are shelves of names in there. Nine hundred with a line through. Lise has no line yet.",
		"He knows we are here. He always knows. Move.",
	};
	public static final String[] FOUND_GAOL = {
		"The prison. Fourteen of us went in to check the cells. Six came out. The rest are still in nine.",
		"Whatever is behind the shut doors does not move while you look at it. So look at it, and keep walking.",
		"The map is in the warder's chest at the end. Then the tunnel. Not at night.",
	};
	public static final String[] FOUND_CHURCH = {
		"The church. He lived under the altar. The priest fed him people and called it prayer.",
		"I killed my brother in this yard. Or something wearing him. I still don't know which.",
		"Our names are on the door. Nineteen of them. We are close now.",
	};
	public static final String[] FOUND_THRESHOLD = {
		"The last house. The door is under the floor. Eight went through on ropes. The ropes came back empty.",
		"Jorunn came back with too many teeth. I put an axe in her here, where you are standing.",
		"It opens one way. Whatever we need, we bring now. I am going through with you.",
	};

	/** A short story, one line at a time, at reading pace, past the quiet timer. */
	public static void tell(ServerLevel here, CompanionEntity her, ServerPlayer to, String[] lines,
	                        int after) {
		int at = after;
		for (String line : lines) {
			final String said = line;
			com.bloomlet.herobrine.manifest.Cadence.in(here.getServer(), at, () -> {
				if (her.isAlive() && !her.isFallen() && to.isAlive()) {
					voice(here, her, said);
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

	/**
	 * HIS VOICE IS THE CHAT LINE, AND EVERYONE ON THAT SIDE OF THE DOOR HEARS IT.
	 * It went to the one player he happened to be talking to, and on a server that
	 * meant the story reached one person and everybody else saw them nod at
	 * nothing. The whole level now: the same words at the same moment.
	 */
	static void voice(ServerLevel here, CompanionEntity her, String line) {
		Component said = Component.literal("§c" + her.getName().getString() + "§7: " + line);
		for (ServerPlayer hearing : here.players()) {
			hearing.sendSystemMessage(said);
		}
		her.lastSpoke = here.getGameTime();
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
					voice(here, her, said);
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
			|| what instanceof InfectedEntity;
	}
}
