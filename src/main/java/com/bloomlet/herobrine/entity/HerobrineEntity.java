package com.bloomlet.herobrine.entity;

import com.bloomlet.herobrine.Config;
import com.bloomlet.herobrine.HerobrineMod;

import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import com.bloomlet.herobrine.wrath.Phase;
import com.bloomlet.herobrine.wrath.Wrath;
import com.bloomlet.herobrine.wrath.Heat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * He does not fight you. He watches, and he is gone when you look.
 *
 * Deliberately NOT a Monster: Monster brings hostile targeting and melee
 * attack goals, which would turn him into an ordinary mob you can kill and
 * stop being afraid of. PathfinderMob gives navigation and goals with no
 * combat, so the only behaviour is the one that makes him unsettling.
 */
public class HerobrineEntity extends PathfinderMob {
	/** Ticks a player has held their gaze on him before he leaves. */
	private int unseenTicks;
	/**
	 * How long he has been looked at, by anybody, since the first pair of eyes
	 * landed on him. ONE clock for the whole event, deliberately.
	 *
	 * Per-player allowances were tried and were wrong, and the reason is worth
	 * keeping. They guaranteed everyone their own second and a half — which
	 * sounds fair and quietly deletes the best thing the sighting has. If both
	 * of you saw him there is nothing to disagree about, and the entire mod is
	 * built on the player not being able to prove what happened.
	 *
	 * The clock belongs to whoever spots him first, and it runs whether or not
	 * anybody else has turned round. Your friend says "he was standing right
	 * there" and you were looking at the wrong hill and there is nothing to
	 * see, and now one of you has to take the other's word for it. Two people
	 * who cannot agree on what was in the clearing is a far worse place to be
	 * than two people who both watched something vanish.
	 */
	private int watchedTicks;
	private int fleeTicks;
	private boolean fleeing;
	/** Ticks since he arrived. */
	private int age;
	/** Whether any player actually laid eyes on him before he left. */
	private boolean witnessed;
	/** Where the player stood when he arrived. He goes back to it. */
	private @org.jspecify.annotations.Nullable BlockPos anchor;
	private int relocations;

	public void setAnchor(BlockPos pos) {
		this.anchor = pos;
	}

	/**
	 * He leaves on his own after this long, seen or not.
	 *
	 * A haunting is a moment. Left indefinitely he becomes scenery — you walk
	 * over, study him, and discover he does nothing, which is the end of being
	 * afraid of him. Better to be gone before the player is certain of what
	 * they saw.
	 */
	private static final int LIFETIME = 600;          // 30 seconds

	/** Get closer than this and he will not let you get closer still. */
	private static final double TOO_CLOSE = 17.0;

	// ---- WHICH SIDE OF THE WAY HE IS ON -----------------------------------
	//
	// THE ONE THING THAT DECIDES WHAT HE IS, AND IT REPLACES THE PHASE LADDER
	// FOR EVERY QUESTION ABOUT HIM.
	//
	// The ladder used to answer "how much Herobrine do you get" — six rungs of
	// slowly increasing presence, unlocked by finding his buildings. That was
	// right when he could be anywhere. It is redundant now that GEOGRAPHY answers
	// it: he lives on the far side of the way and nothing on this side is him
	// being here, it is an apparition. There is nothing left to ration.
	//
	// So the ladder keeps exactly one job — the six buildings, in order, in the
	// overworld — and everything about HIM reads this instead.
	boolean hisGround() {
		return this.level().dimension().equals(
			com.bloomlet.herobrine.block.TheWayBlock.HIS_WORLD);
	}


	/** Arm's length. He is gone before anybody finds out what is here. */
	private static final double ARMS_LENGTH = 3.2;

	// ---- THE HUNT --------------------------------------------------------
	/**
	 * Slower than your sprint, faster than your walk, and that gap is the
	 * whole design.
	 *
	 * Sprinting gets away from him. Walking does not. So the player cannot
	 * ignore him and cannot casually stroll home either — they have to spend
	 * hunger, or they have to hide, and both of those are decisions. A pursuer
	 * you can outwalk is scenery; one you cannot outrun is unfair.
	 */
	private static final double HUNT_SPEED = 1.32;
	/**
	 * A hunt runs longer than a sighting, and is the only thing that does.
	 *
	 * AND A HUNT IS NOT ON A CLOCK AT ALL ANY MORE.
	 *
	 * It was: three minutes, tuned by arithmetic off watchSpell so that three
	 * rounds of pausing would fit inside it. Every part of that reasoning was
	 * sound and the whole thing was still wrong, because it made TIME one of the
	 * six ways out — and a wall clock competing with the third blow is a wall
	 * clock that eventually wins. Two hits in at two minutes fifty-five and the
	 * event was taken away mid-swing, having decided on the player's behalf that
	 * they had had long enough. Worse, the exit counted: the church opened for
	 * it, so waiting was a strategy.
	 *
	 * A hunt is bounded by WHAT HAPPENS IN IT. He reaches somebody, or somebody
	 * reaches him, or a round closes — any of those and he is not finished.
	 * Nothing at all for two minutes and it was never a hunt to begin with, it
	 * was a boat on a lake, and that is the only case this needs to catch.
	 *
	 * So the backstop stops being a length and becomes a STALEMATE, which is
	 * what it was always trying to be. Note what does NOT reset it: the ladder.
	 * The house coming apart is him working on the building rather than on the
	 * player, it fires every thirteen seconds regardless, and letting it count
	 * as progress would mean nothing ever timed out.
	 */
	private int stalemate;
	private static final int STALEMATE_LIMIT = 2400;   // two minutes of nothing

	/** Anything up to this he jumps. Anything above it he goes over. */
	private static final int VAULT_MAX = 4;
	/** How high he will look for a way up before deciding to fly. */
	private static final int SCAN = 10;


	/**
	 * And it is paid ONCE per approach, not once per tick.
	 *
	 * A hundred ticks. Longer than anybody spends inside seventeen blocks of
	 * him by accident, short enough that walking him down, backing off and
	 * walking him down again costs twice — which is the behaviour this is
	 * actually pricing.
	 */
	private static final int DEFIANCE_COOLDOWN = 100;
	/**
	 * Chasing him costs you.
	 *
	 * Walking at him used to be free: he dissolved, you felt powerful, and the
	 * fear was spent. Now closing the distance is read as defiance and raises
	 * wrath sharply — which per LORE.md is precisely the thing that thins the
	 * seal. Players will chase him; the design should make chasing him the
	 * mistake rather than the solution, and it should teach that through
	 * consequence rather than a message.
	 */
	private static final int DEFIANCE_APPROACHED = 25;
	private long lastDefiance = -1000L;
	private boolean flying;
	private int flyTicks;
	/**
	 * THE SKY HAS BEEN TAKEN FROM HIM. Set by the first blow in his castle, never
	 * cleared while he lives, and kept on the level as well so a save or a fresh
	 * one of him finds it. takeOff refuses while it is set — except for the pause
	 * between acts, which is a ceremony and not a tactic. See Duel.
	 */
	private boolean bound;
	private boolean recalled;
	private int helperBlows;
	private java.util.@org.jspecify.annotations.Nullable UUID lastStruckBy;
	private final Duel theDuel = new Duel(this);

	/**
	 * He does not only ever run at you.
	 *
	 * A pursuit that is one unbroken sprint from start to finish is a chase
	 * scene, and a chase scene is exciting rather than frightening — the player
	 * spends it looking forward, solving a movement problem, and never once has
	 * to wonder where he is. So he breaks off. He is suddenly a long way away,
	 * standing still, watching; and then he is not there; and then he is close
	 * again and coming.
	 *
	 * The variety in the DISTANCE is the part that does the work. Something
	 * that is always eight blocks behind you can be modelled. Something that is
	 * forty blocks away and then nine is not.
	 */
	private boolean watching;

	/**
	 * How many times he has broken off, and why it is counted.
	 *
	 * An unbounded watch-and-return loop is what "it is just coming back and
	 * back" means: every cycle is the same size as the last, so there is no
	 * way to tell the second from the fifth, and a thing with no shape reads as
	 * a thing with no end. Three, and each return comes in closer and stays
	 * shorter than the one before, so the player can feel it tightening even
	 * without counting.
	 */
	private int breakOffs;

	/**
	 * Who he has already reached this round.
	 *
	 * A hunt is not a brawl and it is not a chase after whoever happens to be
	 * nearest. He picks one, he gets to them, and then he is finished with them
	 * and turns to somebody who has not been reached yet — round by round,
	 * until everybody has.
	 *
	 * Keyed on UUID rather than holding the entity, so somebody logging out or
	 * dying mid-round cannot pin a reference or block the round from ever
	 * completing.
	 */
	private final java.util.Set<UUID> struck = new java.util.HashSet<>();


	/** He stops, and lets them watch him stop. */
	private boolean relenting;
	/**
	 * HE HAS FINISHED WITH THIS HUNT, AND NOTHING CAN UNSAY IT.
	 *
	 * `relenting` was doing this job and it is not safe for it: takeTheBlow clears
	 * it deliberately, and a playtest log shows a hundred and twenty-nine
	 * "driven off" messages against one completed relent — so the flag was false
	 * again by the time the next blow arrived, and I could not prove from the
	 * source how. Reasoning about it twice got me two wrong answers.
	 *
	 * So this is a latch with exactly one writer for true (relent) and exactly one
	 * for false (beginHunt), and the repeat becomes impossible rather than
	 * unlikely. `relenting` keeps its own job — the two and a half seconds of him
	 * standing there — which is a different question from "is this hunt over".
	 *
	 * The distinction is the lesson from the chest tower and the taunt storm both:
	 * a flag that means "what he is doing right now" cannot also mean "what has
	 * already happened", because the first one has to be allowed to change.
	 */
	private boolean brokenOff;

	// ---- THREE BLOWS AND HE GOES ------------------------------------------
	/**
	 * THE HUNT CAN BE WON, AND THAT IS THE WHOLE OF WHAT WAS MISSING.
	 *
	 * Until now the only thing a player could do about a hunt was outlast it,
	 * hide from it, or run until it timed out — three ways of waiting. Waiting
	 * is not agency, and an event with no ending you can cause is an event the
	 * player is a spectator at. Three blows and he breaks off turns a hundred
	 * seconds of being chased into a fight with a condition on it.
	 *
	 * COUNTED IN BLOWS RATHER THAN IN DAMAGE, for the same reason the Reckoning
	 * is: what somebody is holding decides how the fight LOOKS, never how long
	 * it lasts. A netherite axe and a stone sword end this in the same three
	 * connections, so the player who spent an hour on gear beforehand has not
	 * bought a shorter hunt.
	 *
	 * AND IT SCALES WITH THE GROUP IN THE RIGHT DIRECTION. Six people land three
	 * blows in a few seconds, so a group that stands together ends it almost at
	 * once — which is the correct reward for grouping and costs nothing, because
	 * six people together were never the version of this that was frightening.
	 * One person alone has to land three connections on something faster than a
	 * sprint, in the dark, while their windows are going. That is where the
	 * pressure was supposed to be all along.
	 *
	 * He does not die and never could — see isInvulnerableTo. He is DRIVEN OFF,
	 * which is a different sentence, and the difference is exactly what the
	 * three lines he says on the way out are about.
	 */
	private float huntDamage;

	/**
	 * WHEN HE WAS LAST REACHED, so the client can draw it.
	 *
	 * He has never flashed at all. Vanilla's hurt animation is set inside the hurt
	 * path, and his hurtServer returns false before reaching any of it — so a
	 * player landing a blow on the one thing in the mod they are allowed to fight
	 * got no visual acknowledgement whatever. A hit that looks like nothing reads
	 * as a hit that did nothing.
	 *
	 * A GAME TIME RATHER THAN A COUNTDOWN, and that is the whole design of it. A
	 * ticking-down integer would sync a packet to every client in range on every
	 * one of the eight ticks it takes to fade. A timestamp syncs once, on the blow,
	 * and the client works out how far through the fade it is from its own clock —
	 * which it already has, and which is already in step.
	 *
	 * Not persistent. A save reloaded eight ticks after somebody hit him has
	 * nothing worth remembering.
	 */
	public static final net.fabricmc.fabric.api.attachment.v1.AttachmentType<Long> WOUNDED =
		net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry.<Long>builder()
			.syncWith(net.minecraft.network.codec.ByteBufCodecs.VAR_LONG.cast(),
				net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate.all())
			.buildAndRegister(HerobrineMod.id("wounded_at"));

	/**
	 * Eight ticks. Long enough to register, short enough not to be a costume.
	 *
	 * AND IT IS VANILLA'S RED NOW, NOT A BLACK TINT OF OUR OWN. The custom layer
	 * drew his whole model in black at a third opacity, and the playtest report was
	 * that only his EYES went dark. That is exactly what it would do, and I should
	 * have seen it coming: his skin is nearly black already, so a black tint over
	 * it changes almost nothing — while the eyes are drawn emissive white, so the
	 * one place the darkening had anything to bite on was the two brightest pixels
	 * on the model.
	 *
	 * Black cannot work here at any opacity. It is a darkening, and you cannot
	 * darken something that is already dark. Red works on any texture because it is
	 * a hue shift rather than a subtraction, which is why vanilla picked it.
	 *
	 * hasRedOverlay is a field on the render state, so the body is tinted by the
	 * same code that tints every other mob — and RenderTypes.eyes() ignores the
	 * overlay entirely, so the eyes stay lit through it. Both halves of the report
	 * answered by deleting a layer rather than adding one.
	 */
	public static final int WOUND_FLASH = 8;

	/**
	 * WHEN HE LAST SWUNG — BECAUSE NOTHING ELSE IS KEEPING TRACK.
	 *
	 * LivingEntity.updateSwingTime is the method that advances a swing and sets
	 * attackAnim, which is the only thing the humanoid model reads to move an arm.
	 * In 26.2 it is DEAD ON THE SERVER: it is never called from LivingEntity.tick,
	 * LivingEntity.aiStep, LivingEntity.baseTick or anywhere in Mob. I dumped all
	 * four and there is not one invocation.
	 *
	 * So swing() sent its packet, the gate let it through, and attackAnim sat at
	 * zero — he stood holding a sword and hitting things with an arm that never
	 * moved. Every belt-and-braces fix before this one was tightening a chain
	 * whose last link was missing.
	 *
	 * Driven from here instead, off exactly the pattern the hurt flash already
	 * uses: the server stamps a game time, the client subtracts. Both sides share
	 * the clock, so it costs one long on a swing and nothing at all in between —
	 * and it cannot be broken again by whatever vanilla does with its own.
	 */
	public static final net.fabricmc.fabric.api.attachment.v1.AttachmentType<Long> SWUNG =
		net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry.<Long>builder()
			.syncWith(net.minecraft.network.codec.ByteBufCodecs.VAR_LONG.cast(),
				net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate.all())
			.buildAndRegister(HerobrineMod.id("swung_at"));

	/** Six ticks, which is the length of a vanilla swing. */
	public static final int SWING_SHOWS = 6;

	private int ladder;

	// ---- BREAKING IN ------------------------------------------------------
	/**
	 * A door is not an answer to this any more.
	 *
	 * This is the one place the mod knowingly breaks its own rule about never
	 * touching a player's build (DESIGN.md §9), and the exception is narrow and
	 * deliberate: shelter is the correct answer to almost everything he does,
	 * and at HUNTER it has to stop being one. A pursuer that gives up at a
	 * wooden door is not a pursuer, it is weather.
	 *
	 * THE RULE IS BENT, NOT ABANDONED. Every block he takes out is DROPPED, so
	 * the player loses the wall and their evening and not one item. That is the
	 * same bargain the torches make, and it is what keeps this the wrong side
	 * of frightening rather than the wrong side of griefing.
	 *
	 * And he is slow about it on purpose. The whole value is watching it
	 * happen — hearing the axe go into the door twice while you decide whether
	 * the back window is a better idea. Something that deletes a wall instantly
	 * is a cutscene; something taking eleven seconds through obsidian is a
	 * decision you are being given time to make.
	 */
	private @org.jspecify.annotations.Nullable BlockPos breaking;
	private int breakTicks;

	private static final double BREAK_RANGE = 16.0;
	/**
	 * How far he can reach to swing at something. A player's arm, near enough.
	 *
	 * BREAK_RANGE above is how close the player has to be for him to consider
	 * digging at all; this is how close the BLOCK has to be for him to touch it,
	 * and conflating the two was the whole bug. Slightly longer than vanilla's
	 * three-ish blocks because he is taller than the block he is usually swinging
	 * at and the sightline hits a corner rather than the middle of a face.
	 */
	private static final double REACH = 4.5;
	// ---- END BREAKING IN --------------------------------------------------

	/** Two hearts, and not oftener than once a second. */
	private static final float STRIKE_DAMAGE = 4.0F;
	/**
	 * WHAT A BLOW OF HIS IS WORTH IN THE LAST FIGHT — AND IT GOES THROUGH NOTHING.
	 *
	 * It used to. herobrine:reckoning sat in bypasses_armor and
	 * bypasses_enchantments, so eight points arrived as eight points whatever you
	 * had on, and a full set of netherite was cosmetic. That was put in to answer
	 * a playtest where his hits did nothing, and it answered it by deleting the
	 * armour system rather than by hitting harder.
	 *
	 * The tags are gone. Armour reduces it, Protection reduces it again, the
	 * plates take durability for it, and a shield held up in time stops it and
	 * takes the wear — all four of which are the game working as the player
	 * expects, and all four of which are ways to be GOOD at this fight rather
	 * than merely present at it.
	 *
	 * Twelve, then, instead of eight, because it has to survive the armour:
	 *
	 *     nothing on           12.0    six hearts — one mistake, not death
	 *     iron                  6.7
	 *     diamond               4.1
	 *     netherite             3.6
	 *     netherite, Prot IV    1.3    and the plates are wearing out
	 *
	 * A Prot IV netherite player takes little per hit and that is correct: they
	 * earned it. What they cannot do any more is stand still, because seventy
	 * blows' worth of fight is a long time for durability, and because the
	 * arrows, the fire and the lightning are all still coming.
	 */
	private static final float RECKONING_DAMAGE = 12.0F;

	/** Declared in data/herobrine/damage_type/reckoning.json. */
	private static final net.minecraft.resources.ResourceKey<
			net.minecraft.world.damagesource.DamageType> RECKONING =
		net.minecraft.resources.ResourceKey.create(
			net.minecraft.core.registries.Registries.DAMAGE_TYPE,
			HerobrineMod.id("reckoning"));
	private static final int STRIKE_COOLDOWN = 30;

	/**
	 * AND HE DOES NOT LEAVE THE INSTANT HE HAS HIT YOU.
	 *
	 * He used to. The blow landed and the same tick put him twelve to twenty-two
	 * blocks away, out of the view cone, invisible on the way — so the honest
	 * description of a whole hunt was that something hurt you four times and you
	 * never once had the chance to raise an arm. Three blows was the way out of
	 * the event and the event was structured so that you could not reach him.
	 *
	 * This is the enderman's arrangement, which is the correct one and has been
	 * for a decade: it closes, it stays in melee, and it leaves WHEN IT IS HIT.
	 * The teleport is the reaction to being reached, not a way of preventing it.
	 * So he stands in it for two to three and a half seconds — long enough for a
	 * sword, a bow, or somebody's friend arriving late — and only backs off if
	 * nobody takes it.
	 *
	 * A window rather than a fixed pause because a known length is a known
	 * length, and hitting him should stay a thing you go for rather than a thing
	 * you have timed.
	 */
	private int linger;
	/**
	 * Which of the two events opened it, because they end differently.
	 *
	 * After HE hits, the round closing is what moves him, and it already does —
	 * roundOver repositions him to watch from twenty-six to forty-six. Teleporting
	 * on the way there as well was two blinks in consecutive ticks for one event.
	 *
	 * After he IS hit, nothing else will move him, and something has to: the
	 * provoker override deliberately keeps him on whoever reached him, so a window
	 * that expired without a reposition would leave him standing in swing range of
	 * the one person guaranteed to still be swinging.
	 */
	private boolean lingerWounded;
	private static final int LINGER_MIN = 40;
	private static final int LINGER_SPREAD = 30;

	/** Four seconds. Long enough to matter, short enough not to be the killer. */
	private static final float AXE_BURNS = 4.0F;
	/** Six times vanilla's shove. He is not a zombie. */
	private static final double AXE_SHOVE = 2.4;
	/** Enough to leave the ground, not enough to make the ground the danger. */
	private static final double AXE_LIFT = 0.55;

	/**
	 * WHOEVER JUST SHOT HIM IS WHO HE IS COMING FOR.
	 *
	 * The hunt works through a group one at a time and keeps its quarry until it
	 * has reached them, which is the right default and the reason a fast player
	 * cannot draw him off their friends. It is the wrong answer to being shot in
	 * the back. Somebody puts an arrow in him from forty blocks and he carried on
	 * walking toward a different person entirely — so the correct play in a party
	 * was to let one friend be chased and plink at him from the treeline forever,
	 * which is both the safest thing to do and the least interesting.
	 *
	 * Reaching him now costs you. He drops what he was doing, and he already
	 * teleports beside the striker for the reprisal, so this only makes him STAY
	 * on them once he is there instead of wandering back to his list.
	 *
	 * Ten seconds, then his own order resumes. And it clears their struck flag,
	 * so hitting him puts you back on the list you had got yourself off.
	 */
	private @org.jspecify.annotations.Nullable UUID provoker;
	private int provokedFor;

	private static final int CULL_EVERY = 10;


	/**
	 * Never Long.MIN_VALUE, and this is why he never once hit anybody.
	 *
	 * The guard was `now - lastStruck < STRIKE_COOLDOWN`, and with a sentinel
	 * of Long.MIN_VALUE that subtraction OVERFLOWS: a game time of twelve
	 * thousand minus the most negative long wraps round to about negative nine
	 * quintillion, which is comfortably less than twenty-two. So the cooldown
	 * reported itself as still running on the very first swing, returned early,
	 * and never assigned lastStruck — leaving it wrong forever. He walked up to
	 * players and stood there for three rounds of testing because of a sentinel
	 * value.
	 *
	 * A small negative works because game time only ever counts up from zero,
	 * so nothing here can overflow. The comparison is written as an addition
	 * now as well, which cannot wrap at all.
	 */
	private long lastStruck = -1000L;

	/**
	 * A sighting measured in ticks rather than in seconds.
	 *
	 * Zero means he behaves normally. Anything else is a GLIMPSE — he exists
	 * for that long and then he is not there, regardless of who is looking,
	 * whether they got a good view, or what the phase would otherwise allow.
	 *
	 * The stare is a confrontation you win by looking. This is the opposite and
	 * needs its own timer: the player does not get to resolve it, does not get
	 * long enough to be sure, and is left with the memory rather than the
	 * sighting. "He looked at me and quickly ran into the fog" is the whole of
	 * the original account of meeting him, and it is over in a second.
	 */
	private int glimpseTicks;

	public void beGlimpse(int ticks) {
		this.glimpseTicks = ticks;
	}


	/**
	 * HE DOES NOT DO THIS ON THIS SIDE, AND THAT IS THE WHOLE CHANGE.
	 *
	 * The opening is the run-up to a hunt — he gets behind you, waits to be found,
	 * and turning round starts it. Out here there is nothing for it to run up to:
	 * the fight is in his world, so being spotted in the overworld resolves the only
	 * way an apparition can. He is not there any more.
	 *
	 * Guarding HERE rather than at the hunt is deliberate. Gate the hunt alone and
	 * the player gets the entire approach — the figure behind them, the line in the
	 * chat, the turn — and then nothing, which reads as the mod breaking off. This
	 * way the beat that cannot pay off never starts.
	 */
	private void beginOpening() {
		// Shot at while he stood there. In his castle a player's blow is the fight
		// and never reaches here; out in the world he is an apparition, and an
		// apparition that has been noticed is somewhere else.
		this.vanish("seen, on the wrong side of the way");
	}


	private boolean present;
	private int prowlTicks;
	private int eyeTicks;
	private int wanderIn;
	/** Half a second of him actually having you. */
	private static final int LOCK_ON = 10;
	/**
	 * WHERE HE THINKS IT CAME FROM, and how long he will keep walking at it.
	 *
	 * Fifteen seconds is generous on purpose. It has to be long enough to cross
	 * forty blocks of wood, because the tension is watching him come the whole way
	 * — a search that gives up in four seconds is a noise, not a threat.
	 */
	private @org.jspecify.annotations.Nullable BlockPos suspectAt;
	private int suspicion;
	private static final int LOOKS_FOR = 300;
	/** Six blocks and a clear line, in three dimensions. */
	private static final double CAUGHT_AT = 6.0;
	/** Close enough that a wall is the only reason he cannot see them. */
	private static final double SEALED_IN = 10.0;
	/** Faster than his wander, short of a run. He has not decided yet. */
	private static final double LOOKING_PACE = 0.82;
	/**
	 * How far he notices, and it is two numbers rather than one.
	 *
	 * FORTY IN FRONT, because that is where he is looking and it has to be further
	 * than a player expects — the whole tension of walking near him is not knowing
	 * whether you are already inside it.
	 *
	 * SIXTEEN IN ANY DIRECTION, because a person notices something at their
	 * shoulder without turning round, and a detection cone with a blind spot behind
	 * it turns him into a stealth-game guard you can conga-line behind.
	 *
	 * Crouching halves both. That is the answer the player gets to have, it costs
	 * them their speed, and it is the same bargain the game already teaches.
	 */
	private static final double SEES_FRONT = 24.0;
	private static final double SEES_NEAR = 8.0;
	/** About forty degrees either side. Narrower than a searchlight. */
	private static final double SEES_CONE = 0.72;
	/**
	 * AND CROUCHING IS A REAL ANSWER, NOT A DISCOUNT.
	 *
	 * Halving it left a crouched player visible at twenty blocks, which is not
	 * hiding, it is being seen slightly later. A quarter puts it at six in front
	 * and two beside him — close enough that being caught crouched means you walked
	 * into him, and that is a mistake worth making.
	 *
	 * It costs speed, which is the whole bargain, and it is the bargain the game
	 * already taught them.
	 */
	private static final double CROUCH_HELPS = 0.25;
	/**
	 * AND CROUCHING IS NOT CAMOUFLAGE.
	 *
	 * A quarter is the right discount for a player picking their way through trees
	 * at eighteen blocks. It was the wrong one for a player squatting six blocks in
	 * front of him in an empty field, which the old numbers made completely safe —
	 * crouch cut his near range to two, so you could hold still in the open, in his
	 * eyeline, and be invisible. That is a stealth-game bush, not a person.
	 *
	 * So there is a floor under it. Close, in the open, with a clear line: seen. No
	 * cone, no discount, the same rule a pillager plays by. The answers left are
	 * the real ones — get further away, put something between you, or get under
	 * something.
	 *
	 * AND A TREE IS NOT UNDER SOMETHING. Leaves are checked and skipped, so
	 * climbing gets you height and nothing else. Sky over your head means he can
	 * have you; stone over your head means he cannot.
	 */
	private static final double OPEN_GROUND = 14.0;
	/** A sprint is not sneaking, and he does not need to be looking. */
	private static final double HEARS_SPRINT = 20.0;
	/** And a pick going into stone carries further than a footstep. */
	private static final double HEARS_DIGGING = 26.0;
	/** A minute of it, and then he starts regardless. */
	private static final int PROWL_TICKS = 1200;
	/**
	 * He picks somewhere new to be every four to nine seconds, and goes a long way.
	 *
	 * Eight to thirty-two blocks on a two-to-five second timer meant he never
	 * actually left: the leg expired before he arrived, so he turned, and the net
	 * effect over a minute was a figure milling about one clearing. Somebody who is
	 * exploring covers ground, and somebody FOLLOWING him needs him to.
	 */
	private static final int WANDER_MIN = 80;
	private static final int WANDER_SPREAD = 100;
	/**
	 * A PLAYER'S SPEEDS, not a mob's — AND THEY WERE BOTH TOO SLOW.
	 *
	 * The reason it matters is the whole Herobrine story. Nobody was ever
	 * frightened by a monster patrolling. They were frightened because they saw
	 * SOMEBODY on a hill, assumed it was a player, and then could not explain it.
	 * The instant he moves at a speed no player moves at, the question is answered
	 * and he is a mob.
	 *
	 * 0.42 against his 0.3 movement attribute came out around two and a half blocks
	 * a second — roughly sixty per cent of a walking player, which is the pace of
	 * something browsing, not somebody going anywhere. It read as idle, and idle is
	 * the one thing a person crossing a field never looks.
	 *
	 * So the ladder is set from what a player actually does: a walk at 4.3 blocks a
	 * second, a run at 5.6, and the look-into-it pace in between. He switches
	 * between the two without reason — one leg in four is a sprint.
	 */
	private static final double PROWL_WALK = 0.66;
	private static final double PROWL_SPRINT = 0.95;

	/**
	 * AND HE DOES ORDINARY THINGS WHILE HE IS OUT THERE.
	 *
	 * The single best idea in this file and it costs almost nothing: he chops a log
	 * out of a tree, he plants a sapling, he leaves a crafting table standing in a
	 * field, he stacks three dirt. Exactly the litter a player leaves behind on a
	 * walk, and every one of them is a real block that is still there tomorrow.
	 *
	 * WHY THIS IS THE SCARIEST THING HE DOES. Everything else in the mod is an
	 * event — it happens, it is frightening, and it is over. A crafting table in a
	 * clearing nobody built in is a QUESTION, and it does not expire. The player
	 * finds it a week later and has to decide whether one of their friends put it
	 * there. That is the original story working exactly as it originally worked.
	 *
	 * Never on anything anybody placed, and never inside somebody's build — this is
	 * him wandering past, not him redecorating.
	 */
	private int choreIn;
	private static final int CHORE_MIN = 60;
	private static final int CHORE_SPREAD = 120;

	/**
	 * HE IS PERFORMING, AND NOTHING HE NORMALLY DOES APPLIES.
	 *
	 * Demonstration drives his position, his facing and his timing directly for
	 * seven minutes. Every other mode in this file would fight it for the wheel —
	 * the stare would make him leave when looked at, the prowl would wander him off
	 * the island, the lifetime would delete him halfway through — so the whole
	 * entity stands down and becomes a puppet.
	 *
	 * Which is the correct relationship for a set piece. Nothing about this beat is
	 * emergent and it must not be: it happens the same way for everybody, once.
	 */
	private boolean showing;


	public void beginShowing() {
		this.showing = true;
		this.setNoGravity(true);
		this.setInvulnerable(true);
		this.setPersistenceRequired();
	}

	/**
	 * HE IS HERE. That is all this says, and it never says anything else again.
	 *
	 * It used to mean "he is in wander mode", and it was cleared the moment
	 * anything else started — which is what made the modes exclusive and what
	 * every ordering bug in this file was made of. Nothing clears it now except
	 * him leaving.
	 */
	public void beginProwl() {
		this.recall();
		this.present = true;
		this.wade(false);
		this.prowlTicks = PROWL_TICKS;
		this.eyeTicks = 0;
		this.wanderIn = 0;
		this.choreIn = CHORE_MIN;
		this.suspicion = 0;
		this.suspectAt = null;
		// A new prowl is a new evening. Carrying an errand over would have him set
		// off for a wood that is now four hundred blocks behind him.
		this.errand = null;
		this.homeward = false;
		this.wasFrom = Double.MAX_VALUE;
		this.felling = 0;
		this.haunt = -1;
		this.legTo = null;
	}

	/**
	 * HE IS COMING TO LOOK, AND YOU CAN STILL LEAVE.
	 *
	 * Walks to the spot at a middling pace — faster than his prowl, short of a
	 * sprint, because a thing that breaks into a run has already decided and this
	 * one has not. Head turning the whole way.
	 *
	 * CAUGHT IS A DISTANCE, NOT A TIMER. Six blocks with a clear line, measured in
	 * three dimensions so climbing is a real answer and so is dropping into a hole
	 * — you get away by being somewhere else, which is a thing a player can
	 * actually do, rather than by surviving a countdown they cannot see.
	 *
	 * And when he gets there and it is empty he says so, and goes back to his
	 * evening. Which is the best part: nothing happened, he is still out there, and
	 * you now know exactly how close that was.
	 *
	 * @return true while the search owns the tick
	 */
	private boolean investigate(java.util.List<Player> watchers) {
		if (this.suspectAt == null) {
			this.suspicion = 0;
			return true;
		}
		for (Player watcher : watchers) {
			if (this.distanceTo(watcher) <= CAUGHT_AT && this.hasLineOfSight(watcher)) {
				HerobrineMod.LOGGER.info("he came to look and {} was still there",
					watcher.getName().getString());
				this.suspicion = 0;
				this.beginOpening();
				return false;      // he is still out here. he is just looking at you
			}
		}
		double toMark = Math.sqrt(this.blockPosition().distSqr(this.suspectAt));
		if (toMark > 2.0 && --this.suspicion > 0) {
			this.getNavigation().moveTo(this.suspectAt.getX() + 0.5,
				this.suspectAt.getY(), this.suspectAt.getZ() + 0.5, LOOKING_PACE);
			// Looking at the thing he is walking to. He has a reason now, and a
			// player watching him come should be able to read it off his head.
			this.getLookControl().setLookAt(Vec3.atCenterOf(this.suspectAt));
			return true;
		}
		// EXCEPT SOMEBODY IS IN THERE.
		//
		// The shelling was locked inside the hunt, which left the exact case it
		// exists for wide open: seal yourself in BEFORE he starts one and he walks
		// to the mark, says "nothing", puts a torch down and goes home. Camping
		// worked perfectly as long as you did it early enough.
		//
		// He came to look. If the reason he cannot find anybody is a roof, the roof
		// is the thing he has a problem with — and it is the same answer as in a
		// hunt because it is the same question.
		if (this.level() instanceof ServerLevel dug && Config.get().breakIn) {
			for (Player hiding : watchers) {
				// HE OPENS THE PLACE HE CAME TO, NOT THE PLACE THEY ARE.
				//
				// The old test was "close, and I cannot see them, and they are not
				// under the sky" — which is a description of a player being hidden,
				// used as proof of where they were hidden. He walked to a noise and
				// then shelled a room he had no way of knowing about.
				//
				// The mark is what he has: suspectAt, the spot the sound came from.
				// If THAT is enclosed, that is what comes in. Being still in it is
				// the player's business.
				if (!(hiding instanceof ServerPlayer sealed)
					|| this.suspectAt == null
					|| this.distanceTo(hiding) > SEALED_IN
					|| this.hasLineOfSight(hiding)
					|| dug.canSeeSky(this.suspectAt.above())) {
					continue;
				}
				HerobrineMod.LOGGER.info(
					"he could not find {} and opened [{}, {}, {}] instead",
					sealed.getName().getString(),
					this.suspectAt.getX(), this.suspectAt.getY(), this.suspectAt.getZ());
				this.swipe();
				com.bloomlet.herobrine.manifest.TheHunt.shell(dug, this, sealed,
					this.suspectAt);
				this.suspicion = 0;
				this.suspectAt = null;
				this.getNavigation().stop();
				return true;
			}
		}

		// Arrived, or given up on the way. Either way there is nothing here.
		if (this.level() instanceof ServerLevel here) {
			com.bloomlet.herobrine.manifest.TheHunt.suspects(here, this.suspectAt, true);
			this.markIt(here, this.suspectAt);
		}
		this.suspicion = 0;
		this.suspectAt = null;
		this.getNavigation().stop();
		return true;
	}

	/**
	 * AND HE LEAVES A MARK WHERE HE FOUND NOTHING.
	 *
	 * "Nothing" was genuinely nothing — he said a line into the dark and went back
	 * to his evening, and by morning the whole thing had never happened. Which
	 * wastes the best outcome in the mod: the near miss, the one where you got away
	 * and only you know it.
	 *
	 * A redstone torch, because of what it is. It is the dimmest light in the game,
	 * it is RED, it does not occur naturally anywhere on the surface, and no player
	 * has ever placed one in a field by accident. It cannot be mistaken for
	 * anything except somebody standing there and putting it down.
	 *
	 * And it is permanent. Over a long save the map fills up with little red points
	 * — every place he came looking and you were not there — and reading that map
	 * back is reading how close it has been all along.
	 */
	private void markIt(ServerLevel here, BlockPos at) {
		for (int attempt = 0; attempt < 10; attempt++) {
			BlockPos spot = at.offset(this.random.nextInt(3) - 1,
				this.random.nextInt(2), this.random.nextInt(3) - 1);
			if (!here.getBlockState(spot).isAir()
				|| !here.getBlockState(spot.below()).isSolid()
				|| com.bloomlet.herobrine.manifest.DwellTracker.isBuilt(here, spot)
				|| this.getBoundingBox().intersects(
					new net.minecraft.world.phys.AABB(spot))) {
				continue;
			}
			here.setBlock(spot, Blocks.REDSTONE_TORCH.defaultBlockState(), 3);
			HerobrineMod.LOGGER.info("he left one standing at [{}, {}, {}]",
				spot.getX(), spot.getY(), spot.getZ());
			return;
		}
	}

	/**
	 * HIS HEAD GOES BEFORE THE REST OF HIM.
	 *
	 * A person who hears a pick two hundred blocks away does one thing first, and
	 * it takes a quarter of a second: they look at it. Then they carry on, or they
	 * come. The looking is free and it is the entire tell — a figure that turns its
	 * head toward the exact block you just broke has heard you, and you know it
	 * before he has taken a step.
	 *
	 * Once per noise. snappedTo remembers what he has already answered, so a player
	 * mining a vein gets one turn of the head rather than a metronome, and the next
	 * sound from somewhere else gets its own.
	 *
	 * And it is a lock, not a glance — ninety degrees a tick, so it SNAPS. Then it
	 * lets go after a second and a half and his head goes back down the path.
	 */
	private @org.jspecify.annotations.Nullable BlockPos legTo;
	private @org.jspecify.annotations.Nullable BlockPos snappedTo;
	private int headTicks;
	private static final int HEAD_HOLDS = 30;

	private void listen() {
		if (this.level() instanceof ServerLevel ears) {
			BlockPos noise = com.bloomlet.herobrine.manifest.Whereabouts.heard(ears);
			if (noise == null) {
				// The window closed. Forgetting it here is what lets the NEXT swing
				// at the same block count as a new sound rather than the old one.
				this.snappedTo = null;
			} else if (!noise.equals(this.snappedTo)
				&& Math.sqrt(this.blockPosition().distSqr(noise)) < HEARS_DIGGING) {
				this.snappedTo = noise;
				this.headTicks = HEAD_HOLDS;
			}
		}
		if (this.headTicks > 0 && this.snappedTo != null) {
			this.headTicks--;
			this.getLookControl().setLookAt(
				this.snappedTo.getX() + 0.5, this.snappedTo.getY() + 0.5,
				this.snappedTo.getZ() + 0.5, 90.0F, 90.0F);
		}
	}

	/**
	 * HE GETS STUCK, AND HE GETS HIMSELF OUT.
	 *
	 * Two different failures that look identical from outside — a figure standing
	 * in a wood not moving — and they need opposite answers.
	 *
	 * BOXED IN. Something is inside his body: leaves he walked up into on a 1.6
	 * step height, a course of his own wall, grass that grew back. He tears it
	 * out, which is both the fix and the correct thing for him to do — a person in
	 * that situation swings at it.
	 *
	 * MAROONED. Nothing is inside him; he is standing ON something he cannot path
	 * off, which in practice is always a canopy. The test for it is free: topOf
	 * already refuses to count logs and leaves as footing, so if the ground it
	 * reports is three or more blocks below his feet, he is up a tree. He comes
	 * down.
	 *
	 * WHY THE TWO ARE SEPARATE. A single "has not moved in five seconds" rule tore
	 * blocks out around him every time he simply stood still between legs, which
	 * is most of a prowl. Neither of these fires unless he is actually stuck: one
	 * needs geometry in his chest, the other needs open air under his feet.
	 */
	private static final int BOXED_AFTER = 40;
	private static final int MAROONED_AFTER = 200;
	private static final int UP_A_TREE = 3;
	/** And how far under it before being down there counts as being trapped. */
	private static final int DOWN_A_HOLE = 5;
	private @org.jspecify.annotations.Nullable BlockPos wasAt;
	private int stillTicks;
	private int boxedTicks;

	private void unwedge(ServerLevel around) {
		int torn = 0;
		if (this.inSomething(around)) {
			if (++this.boxedTicks >= BOXED_AFTER) {
				this.boxedTicks = 0;
				torn = this.tearOut(around);
			}
		} else {
			this.boxedTicks = 0;
		}

		BlockPos now = this.blockPosition();
		if (this.wasAt == null || now.distSqr(this.wasAt) > 2.0) {
			this.wasAt = now;
			this.stillTicks = 0;
			return;
		}
		if (++this.stillTicks < MAROONED_AFTER || torn > 0) {
			return;
		}
		this.stillTicks = 0;
		int floor = com.bloomlet.herobrine.structure.Ground.topOf(
			around, now.getX(), now.getZ()) + 1;
		int off = now.getY() - floor;
		if (off >= UP_A_TREE) {
			this.snapTo(now.getX() + 0.5, floor, now.getZ() + 0.5, this.getYRot(), 0.0F);
			this.getNavigation().stop();
			this.wanderIn = 0;
			HerobrineMod.LOGGER.info(
				"prowl: he was up a tree at [{}, {}, {}] and came down to {}",
				now.getX(), now.getY(), now.getZ(), floor);
			return;
		}
		// AND THE OTHER WAY, WHICH IS THE ONE THAT KEPT HAPPENING.
		//
		// This test only ever looked UP. Underground the number is negative, so it
		// returned on the first line every single time — which meant a cave, a
		// ravine, a pit or somebody's quarry had NOTHING watching it. He would step
		// down three, find he could only climb one, and walk the bottom of the hole
		// until a player wandered far enough away to delete him.
		//
		// Down here he does not climb out by hand: the way up may be forty blocks
		// of stone. He steps through instead, and only if nobody can see him — and
		// if somebody can, he stays down there, which is at least honest.
		if (off <= -DOWN_A_HOLE) {
			HerobrineMod.LOGGER.info("prowl: he is {} under the ground at [{}, {}, {}]",
				-off, now.getX(), now.getY(), now.getZ());
			BlockPos out = this.legTo != null ? this.legTo
				: new BlockPos(now.getX(), floor, now.getZ());
			if (!this.slipTo(out)) {
				this.wanderIn = 0;
			}
			return;
		}
		// On the ground and idle. Not stuck — just done with this leg early.
		this.getNavigation().stop();
		this.wanderIn = 0;
	}

	/**
	 * A PLACE HE CANNOT WALK TO IS NOT A PLACE HE IS GOING.
	 *
	 * Two ways an errand dies and neither used to be noticed. The pathfinder can
	 * refuse outright, which moveTo reports by returning false. Or — the one that
	 * actually happens — it returns a PARTIAL path: it routes him to the foot of
	 * the mountain, he walks there, the navigation finishes, and he stands against
	 * the rock still sixty blocks short with a destination he will re-issue every
	 * leg for the rest of the evening.
	 *
	 * THE TEST IS PROGRESS, NOT "IS THE PATH FINISHED".
	 *
	 * Finished-and-far is true of every long walk: vanilla truncates a path at
	 * follow range, so a hundred-block errand always arrives at a stopping point
	 * short of the target and the next leg picks up from there. Killing the errand
	 * on that would have killed every errand over ninety-six blocks.
	 *
	 * Closing the distance at all — half a block since the last time he did — resets
	 * it. Five seconds of gaining nothing is a wall. He is not a mob stuck on a
	 * fence; he is somebody who looked at a mountain and changed their mind.
	 */
	private static final int NO_ROUTE_AFTER = 100;
	private int noRoute;
	private double wasFrom = Double.MAX_VALUE;

	private void giveUpOnUnreachable() {
		// EVERY LEG, NOT JUST THE ERRAND.
		//
		// This watched errands only, so a route stop he could not reach and a
		// random leg into a river had nothing on them at all. And the errand is the
		// rarest of the three — one roll in six, behind a four-minute floor — so
		// the one destination being supervised was the one he almost never had.
		//
		// Watching legTo covers all three, and it is the same test: closing the
		// distance resets the clock, and not closing it is a wall. A water flow is
		// the case that proves the point — he is MOVING the whole time, at speed,
		// so nothing that asks "has he stopped" will ever notice, and the distance
		// to where he was going goes up and up.
		BlockPos to = this.errand != null ? this.errand : this.legTo;
		if (to == null || this.blockPosition().closerThan(to, AT_THE_TREELINE)) {
			this.noRoute = 0;
			this.wasFrom = Double.MAX_VALUE;
			return;
		}
		double from = Math.sqrt(this.blockPosition().distSqr(to));
		if (from < this.wasFrom - 0.5) {
			this.wasFrom = from;
			this.noRoute = 0;
			return;
		}
		if (++this.noRoute <= NO_ROUTE_AFTER) {
			return;
		}
		this.noRoute = 0;
		this.wasFrom = Double.MAX_VALUE;
		// ONE MORE ANSWER BEFORE HE GIVES UP, AND ONLY IF NOBODY IS LOOKING.
		if (this.slipTo(to)) {
			return;
		}
		HerobrineMod.LOGGER.info("prowl: no way through to [{}, {}, {}] — he gives it up",
			to.getX(), to.getY(), to.getZ());
		if (this.errand != null) {
			this.errand = null;
			this.homeward = false;
			this.felling = 0;
		} else {
			// A route stop he cannot reach: take the next one rather than spend
			// the evening walking at the same door.
			this.tries = GIVES_UP_AFTER + 1;
		}
		this.legTo = null;
		this.wanderIn = 0;
	}

	/**
	 * HE STEPS THROUGH, AND ONLY WHERE IT CANNOT BE SEEN.
	 *
	 * A wider A* budget solves most hard geometry and it will never solve all of
	 * it: a sealed cellar with a ladder out, the far lip of a ravine, the inside of
	 * somebody's base. The general answer for those is the oldest thing in his
	 * story — he was over there, and now he is over here, and nobody watched it
	 * happen.
	 *
	 * THE WATCHING IS THE WHOLE RULE, and without it this feature would ruin the
	 * mod. Everything good about him depends on his being a person who TRAVELS:
	 * follow him home, be at the tower before he is, watch him cross a field. A
	 * figure that blinks past obstacles cannot be followed and his route stops
	 * being readable. So: seen, and he walks it or he fails at it in front of you.
	 * Unseen, and the problem was never a problem.
	 *
	 * A LAST RESORT, not a way of getting about. It fires only after the navigator
	 * has spent five seconds gaining nothing, only to somewhere he could stand,
	 * only where there is a route onward — no point arriving in a second sealed
	 * room — and never inside somebody's render of him.
	 */
	private static final int SLIPS_EVERY = 600;
	private static final double NEVER_NEARER = 24.0;
	private int slipAt;

	private boolean slipTo(BlockPos want) {
		if (!(this.level() instanceof ServerLevel here) || this.age < this.slipAt) {
			return false;
		}
		java.util.List<Player> near = here.getEntitiesOfClass(Player.class,
			this.getBoundingBox().inflate(WATCH_RANGE),
			who -> who.isAlive() && !who.isSpectator());
		for (Player watcher : near) {
			// Eyes on him, or simply close enough that a man ceasing to be there is
			// something you would notice out of the corner of one. Facing away is
			// not the same as not looking.
			if (this.inViewOf(watcher) || this.distanceTo(watcher) < NEVER_NEARER) {
				return false;
			}
		}
		for (int attempt = 0; attempt < 24; attempt++) {
			BlockPos at = want.offset(
				this.random.nextInt(9) - 4,
				this.random.nextInt(5) - 2,
				this.random.nextInt(9) - 4);
			if (!com.bloomlet.herobrine.entity.ConfinedPlacement.canStand(here, at)) {
				continue;
			}
			boolean crowded = false;
			for (Player watcher : near) {
				if (watcher.distanceToSqr(at.getX() + 0.5, at.getY(), at.getZ() + 0.5)
						< NEVER_NEARER * NEVER_NEARER) {
					crowded = true;
					break;
				}
			}
			if (crowded) {
				continue;
			}
			// AND SOMEWHERE HE CAN GET ON FROM. Arriving inside a second sealed
			// room is the same failure one step further along, and he would spend
			// the rest of the evening stepping between two of them.
			BlockPos was = this.blockPosition();
			this.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5,
				this.getYRot(), 0.0F);
			if (this.getNavigation().createPath(want, 1) == null) {
				this.snapTo(was.getX() + 0.5, was.getY(), was.getZ() + 0.5,
					this.getYRot(), 0.0F);
				continue;
			}
			this.getNavigation().stop();
			this.wanderIn = 0;
			this.slipAt = this.age + SLIPS_EVERY;
			HerobrineMod.LOGGER.info(
				"nobody was looking: [{}, {}, {}] to [{}, {}, {}]",
				was.getX(), was.getY(), was.getZ(), at.getX(), at.getY(), at.getZ());
			return true;
		}
		return false;
	}

	/**
	 * HE CLIMBS LADDERS, BECAUSE A PERSON DOES.
	 *
	 * Ground pathfinding generates neighbours one block up and one block down, so a
	 * nine-block shaft is not a route — it is a wall with a hole in the top. Which
	 * means the passage under his own land, the best thing he owns, was somewhere
	 * he could not physically get to: both ways in are ladders.
	 *
	 * The CLIMBING is free — LivingEntity already gives anything touching a
	 * climbable the same physics a player gets, and jumping against a rung goes up.
	 * The only thing missing was the intent. So when what he wants is well above or
	 * below him and there is a rung within reach, he stops asking the navigator and
	 * uses it, exactly the way you would.
	 *
	 * Four blocks of difference before it engages, so a doorstep, a kerb or a
	 * one-block ledge is still walked rather than climbed.
	 */
	private static final int WORTH_CLIMBING = 4;
	private static final double DESCENDS = 0.15;

	private boolean ladders(@org.jspecify.annotations.Nullable BlockPos to) {
		if (to == null) {
			return false;
		}
		int dy = to.getY() - this.getBlockY();
		if (Math.abs(dy) < WORTH_CLIMBING) {
			this.setJumping(false);
			return false;
		}
		BlockPos rung = this.rungNear();
		if (rung == null) {
			this.setJumping(false);
			return false;
		}
		this.getNavigation().stop();
		double cx = rung.getX() + 0.5;
		double cz = rung.getZ() + 0.5;
		double offX = cx - this.getX();
		double offZ = cz - this.getZ();
		double off = Math.sqrt(offX * offX + offZ * offZ);
		this.getLookControl().setLookAt(cx, this.getEyeY(), cz);
		// Onto it first. A rung you are standing beside is not a rung you are on.
		if (off > 0.4) {
			this.setDeltaMovement(offX / off * 0.12,
				this.getDeltaMovement().y, offZ / off * 0.12);
			return true;
		}
		if (dy > 0) {
			// Vanilla clamps a climber to a fifth of a block a tick, so this is
			// exactly the speed a player goes up a ladder. Nothing to tune.
			this.setJumping(true);
		} else {
			this.setJumping(false);
			this.setDeltaMovement(0.0, -DESCENDS, 0.0);
		}
		return true;
	}

	/** A rung in his own square or one of the four beside it, at foot or head. */
	private @org.jspecify.annotations.Nullable BlockPos rungNear() {
		BlockPos feet = this.blockPosition();
		for (int up = 0; up <= 1; up++) {
			BlockPos level = feet.above(up);
			if (this.level().getBlockState(level).is(net.minecraft.tags.BlockTags.CLIMBABLE)) {
				return level;
			}
			for (net.minecraft.core.Direction way
					: net.minecraft.core.Direction.Plane.HORIZONTAL) {
				BlockPos at = level.relative(way);
				if (this.level().getBlockState(at).is(net.minecraft.tags.BlockTags.CLIMBABLE)) {
					return at;
				}
			}
		}
		return null;
	}

	/** Is there geometry in his chest? */
	private boolean inSomething(ServerLevel around) {
		net.minecraft.world.phys.AABB body = this.getBoundingBox().deflate(0.12);
		for (BlockPos at : BlockPos.betweenClosed(
				BlockPos.containing(body.minX, body.minY, body.minZ),
				BlockPos.containing(body.maxX, body.maxY, body.maxZ))) {
			net.minecraft.world.level.block.state.BlockState in = around.getBlockState(at);
			if (in.isAir() || in.getCollisionShape(around, at).isEmpty()) {
				continue;
			}
			if (body.intersects(new net.minecraft.world.phys.AABB(at))) {
				return true;
			}
		}
		return false;
	}

	/** @return how much of it came out */
	private int tearOut(ServerLevel around) {
		net.minecraft.world.phys.AABB body = this.getBoundingBox().deflate(0.12);
		java.util.List<BlockPos> wedged = new java.util.ArrayList<>();
		for (BlockPos at : BlockPos.betweenClosed(
				BlockPos.containing(body.minX, body.minY, body.minZ),
				BlockPos.containing(body.maxX, body.maxY, body.maxZ))) {
			if (!this.diggable(around, at)
				|| around.getBlockState(at).getCollisionShape(around, at).isEmpty()
				|| !body.intersects(new net.minecraft.world.phys.AABB(at))) {
				continue;
			}
			wedged.add(at.immutable());
		}
		if (wedged.isEmpty()) {
			return 0;
		}
		this.swipe();
		for (BlockPos at : wedged) {
			around.destroyBlock(at, false);
		}
		this.getNavigation().stop();
		this.wanderIn = 0;
		HerobrineMod.LOGGER.info("prowl: he tore {} out from around himself at [{}, {}, {}]",
			wedged.size(), this.getBlockX(), this.getBlockY(), this.getBlockZ());
		return wedged.size();
	}

	/**
	 * A GAP OR A LEDGE ON THE WAY TO WHEREVER HE IS GOING.
	 *
	 * The same two moves the hunt makes, on the same two conditions, against the
	 * current leg instead of against a person. Mob pathfinding will not jump a
	 * two-block gap, so a stream, a ditch, a doorway with the floor out or the
	 * space between two roots simply stopped him — and stopping is why he read as
	 * passive. Half the time he was not idle, he was beaten by a step.
	 */
	private void overIt() {
		if (this.legTo == null || !this.onGround()) {
			return;
		}
		double tx = this.legTo.getX() + 0.5;
		double tz = this.legTo.getZ() + 0.5;
		if (this.blockPosition().closerThan(this.legTo, 2.0)) {
			return;
		}
		// FIRST, IS IT THE KIND OF THING THAT OPENS.
		//
		// openInstead already knows how to work a door, a wooden trapdoor and a
		// fence gate — everything a player can open by hand, with iron correctly
		// refused. And both of its callers were inside the hunt, reached only as a
		// side effect of him deciding to CHOP something. So the full set existed
		// and a wandering Herobrine could use exactly one of it: doors, via the
		// vanilla goal, and only doors.
		//
		// A trapdoor over a shaft and a gate into a garden were both walls to him.
		// The pathfinder will not even route through a closed gate — PathType.FENCE
		// is blocked and covers fences, walls and shut gates alike — so this is the
		// only place it can be answered: at the block, on foot, by hand.
		//
		// He leaves all of it open behind him, which is the point.
		if (this.level() instanceof ServerLevel here) {
			Vec3 step = new Vec3(tx - this.getX(), 0.0, tz - this.getZ()).normalize();
			BlockPos ahead = BlockPos.containing(
				this.getX() + step.x, this.getY(), this.getZ() + step.z);
			if (this.swings(here, ahead) || this.swings(here, ahead.above())) {
				return;
			}
		}
		if (this.leap(tx, tz)) {
			return;
		}
		int wall = this.wallAhead(tx, tz);
		if (wall > 0 && wall <= VAULT_MAX) {
			this.vault(tx, tz, wall);
		}
	}

	/** Inside this and he is on his own land, not out in the world. */
	private static final double NEAR_HOME = 100.0;

	/**
	 * ON HIS OWN LAND HE WALKS IT, IN ORDER.
	 *
	 * A ring at a random bearing was the first attempt and it was still a wander —
	 * he circled the house without ever going to anything, and the shed, the cellar
	 * and the tower stayed places nobody ever saw him at.
	 *
	 * This is a ROUTE. Whereabouts.haunts lists his stops and he takes them in
	 * sequence, one per leg, and does not advance until he has actually arrived —
	 * so a leg that expires halfway leaves him still headed for the same door.
	 *
	 * The sequence is the whole point. After two laps a player knows where he is
	 * going next, which means they can be there first, or make very sure they are
	 * not. Predictability is not a failure of a stalker; it is the thing that makes
	 * one playable.
	 *
	 * @return the next stop, or null if he is nowhere near home
	 */
	private int haunt = -1;
	private int tries;
	/** Legs spent on one stop before he gives up on it and takes the next. */
	private static final int GIVES_UP_AFTER = 4;

	private @org.jspecify.annotations.Nullable BlockPos nextHaunt() {
		if (!(this.level() instanceof ServerLevel here)) {
			return null;
		}
		BlockPos home = com.bloomlet.herobrine.manifest.Whereabouts.home(here);
		if (home == null || !home.closerThan(this.blockPosition(), NEAR_HOME)) {
			return null;
		}
		java.util.List<BlockPos> route =
			com.bloomlet.herobrine.manifest.Whereabouts.haunts(here);
		if (route.isEmpty()) {
			return null;
		}
		if (this.haunt < 0 || this.haunt >= route.size()) {
			this.haunt = 0;
		}
		// ARRIVED, OR GIVEN UP ON IT.
		//
		// Two of the stops are underground and one is across a field, and vanilla
		// navigation will not always find them. Advancing only on arrival meant one
		// unreachable door deadlocked the whole route on it forever — he would head
		// for the cellar every leg for the rest of the save and never reach it, and
		// nothing else on his land would ever get visited again.
		if (this.blockPosition().closerThan(route.get(this.haunt), 5.0)
			|| ++this.tries > GIVES_UP_AFTER) {
			this.haunt = (this.haunt + 1) % route.size();
			this.tries = 0;
		}
		return route.get(this.haunt);
	}


	// ---- END THE MARK -----------------------------------------------------

	// ---- OVER THE KEEP ----------------------------------------------------
	//
	// HE LIVES IN HIS OWN WORLD AND CIRCLES HIS OWN CASTLE.
	//
	// The whole shape of the mod turns on this. He was resident in the OVERWORLD —
	// a house, a tower, a property round, errands — which put the strongest thing
	// in the game on the wrong side of the door, and everything that followed was a
	// consequence: the overworld had to be survivable so he had to be defanged, the
	// atmosphere could not run because he was already standing in it, and the
	// dimension was somewhere you went once and never again.
	//
	// So he is over there, and the way through is the only way to him. What is on
	// this side is his HOUSE — empty, weathered, with his address on a map — and
	// the traces, and the stares. None of which can kill anybody.
	//
	// AND HE IS IN THE AIR, WHICH IS NOT DECORATION. On the ground he is a figure
	// you can be at arm's length from before you have decided anything. Twenty-four
	// blocks up, circling, is a thing you see from the landing and walk toward for
	// two minutes knowing exactly what it is — and the whole of that walk is spent
	// looking up at it. It is also the honest reading of somewhere he owns: he has
	// nothing to hide from and nothing to sneak up on.

	/** How high over the keep floor he holds. */
	private static final double PATROL_UP = 24.0;
	/** And the ring, re-rolled every eight to eighteen seconds. */
	private static final double PATROL_IN = 16.0;
	private static final double PATROL_OUT = 34.0;
	/** Slower than the duel's ring. He is not chasing anything. */
	/**
	 * Blocks a tick along the ring, at any radius. 4.4 a second.
	 *
	 * Deliberately under a sprint. He is not chasing anybody up here and he should
	 * not be able to — what the pace has to buy is TIME TO BE LOOKED AT, and a
	 * circuit somebody can watch a whole lap of. Faster than the player made him a
	 * thing that went past.
	 */
	private static final double PATROL_PACE = 0.22;
	/** And how far he stays above whatever is actually underneath him. */
	private static final double PATROL_CLEARS = 7.0;
	/** How far off he still turns his head to somebody. */
	private static final double PATROL_NOTICES = 72.0;

	/**
	 * @return true when this owns the tick
	 */
	private boolean patrol() {
		// Anything with his attention outranks it — a duel, the opening, a hunt.
		// This is what he does when nothing does.
		if (this.bound || this.hunting || this.busyWith != null || this.fleeing
			|| !(this.level() instanceof ServerLevel his)
			|| !his.dimension().equals(
				com.bloomlet.herobrine.block.TheWayBlock.HIS_WORLD)) {
			return false;
		}
		BlockPos keep = com.bloomlet.herobrine.structure.Keep.site(his);
		if (keep == null) {
			return false;      // nothing built yet, so nothing to circle
		}
		this.takeOff();
		this.setNoGravity(true);
		this.setDeltaMovement(Vec3.ZERO);
		if (--this.orbitFor <= 0) {
			this.orbitFor = 160 + this.random.nextInt(200);
			this.orbitWay = this.random.nextBoolean() ? 1 : -1;
			this.orbitWide = PATROL_IN + this.random.nextDouble() * (PATROL_OUT - PATROL_IN);
		}
		// CONSTANT SPEED OVER THE GROUND, NOT CONSTANT ANGLE.
		//
		// It used to advance a fixed number of radians a tick, which means the
		// LINEAR speed scaled with whatever radius the current leg had rolled: 6.2
		// blocks a second on the inside of the ring and 13.1 on the outside. A
		// sprinting player does 5.6. So on a wide leg he crossed the sky at better
		// than twice anything on the ground could follow, in a couple of seconds,
		// and was gone — which is exactly how it was reported.
		//
		// Dividing by the radius makes the angle the derived quantity and the pace
		// the fixed one. Every leg now reads the same from below, and a circuit
		// takes long enough to watch him come round again.
		this.orbit += this.orbitWay * (PATROL_PACE / this.orbitWide);
		// A long slow rise and fall on top of the circle, so the path never closes
		// on itself and he never reads as being on rails.
		double want = keep.getY() + PATROL_UP + Math.sin(this.age * 0.015) * 4.0;
		// AND NEVER INSIDE THE HILL.
		//
		// The height was measured off the KEEP and nothing else, while the ring
		// runs out to thirty-four blocks — and Keep.highest only sampled twenty-four
		// when it chose the site. So ground beyond that could be, and was, higher
		// than the flight path. He has no gravity and no physics up here, so he did
		// not land on it or bounce off it: he flew straight through the rock.
		//
		// Which costs far more than it looks. There is no line of sight into a
		// hillside, and being SHOT is the one thing that takes him off patrol. A
		// player who cannot see him cannot start the fight, so an aesthetic problem
		// was silently a progression one.
		int under = his.getHeight(
			net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
			net.minecraft.util.Mth.floor(this.getX()),
			net.minecraft.util.Mth.floor(this.getZ()));
		want = Math.max(want, under + PATROL_CLEARS);
		double toX = keep.getX() + 0.5 + Math.cos(this.orbit) * this.orbitWide;
		double toZ = keep.getZ() + 0.5 + Math.sin(this.orbit) * this.orbitWide;
		// Facing along the circle, not at the middle of it — a man flying sideways
		// round a tower is a prop; a man flying the way he is going is flying.
		float bearing = (float) Math.toDegrees(
			-Math.atan2(toX - this.getX(), toZ - this.getZ()));
		this.snapTo(
			this.getX() + net.minecraft.util.Mth.clamp(
				toX - this.getX(), -HOVER_PACE, HOVER_PACE),
			this.getY() + net.minecraft.util.Mth.clamp(want - this.getY(), -0.3, 0.3),
			this.getZ() + net.minecraft.util.Mth.clamp(
				toZ - this.getZ(), -HOVER_PACE, HOVER_PACE),
			bearing, 0.0F);
		// AND THE HEAD GOES WITH WHOEVER TURNED UP. The body keeps its bearing, so
		// what the player sees from the ground is somebody who has not altered
		// course by a degree and is looking straight down at them.
		Player below = his.getNearestPlayer(this, PATROL_NOTICES);
		if (below != null) {
			this.getLookControl().setLookAt(below, 90.0F, 90.0F);
		}
		return true;
	}
	// ---- END OVER THE KEEP ------------------------------------------------

	/** Has he got them — in front and in range, or simply too close to miss? */
	private boolean spots(Player them) {
		if (!this.hasLineOfSight(them)) {
			return false;
		}
		double away = this.distanceTo(them);
		// Close, out in it, and nothing in the way. Nothing else is consulted.
		if (away <= OPEN_GROUND && underTheSky(them)) {
			return true;
		}
		double front = SEES_FRONT;
		double near = SEES_NEAR;
		if (them.isCrouching()) {
			front *= CROUCH_HELPS;
			near *= CROUCH_HELPS;
		}
		if (away > front) {
			return false;
		}
		if (away <= near) {
			return true;
		}
		Vec3 look = this.getViewVector(1.0F).normalize();
		Vec3 toThem = new Vec3(them.getX() - this.getX(),
			them.getEyeY() - this.getEyeY(), them.getZ() - this.getZ()).normalize();
		return look.dot(toThem) > SEES_CONE;
	}

	/**
	 * IS THERE SKY OVER THEM — counting a tree as sky, because it is.
	 *
	 * canSeeSky on its own says no for anybody standing under a canopy, which would
	 * hand every player in a forest the same cover a cave gives them. So when the
	 * heightmap says something is up there, the column is walked and leaves are
	 * skipped: air and foliage all the way up is still out in the open, and one
	 * solid block is a roof.
	 *
	 * Bounded by the heightmap rather than the build limit, so it is a handful of
	 * lookups and never a scan of the sky.
	 */
	private boolean underTheSky(Player them) {
		Level here = this.level();
		BlockPos head = BlockPos.containing(them.getX(), them.getEyeY(), them.getZ());
		if (here.canSeeSky(head)) {
			return true;
		}
		int top = here.getHeight(
			net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
			head.getX(), head.getZ());
		for (int y = head.getY() + 1; y <= top; y++) {
			net.minecraft.world.level.block.state.BlockState above =
				here.getBlockState(new BlockPos(head.getX(), y, head.getZ()));
			if (above.isAir()
				|| above.is(net.minecraft.tags.BlockTags.LEAVES)
				|| !above.isSolid()) {
				continue;
			}
			return false;
		}
		return true;
	}

	/**
	 * WHAT HE CAN HEAR, WHICH IS NOW MOST OF HOW HE FINDS ANYBODY.
	 *
	 * Two things, and both are choices the player made rather than angles they
	 * happened to be standing at.
	 *
	 * SPRINTING, at twenty blocks, through walls and behind him. Running is the
	 * loudest thing in the game and it is also what somebody does the instant they
	 * are frightened — which makes the correct move under pressure the one that
	 * gets you caught, and that is the best kind of rule.
	 *
	 * DIGGING, at twenty-six, from wherever the block came out. It carries further
	 * than a footstep because a pick going into stone does. Crouching does not help
	 * with either: you cannot sneak a pickaxe.
	 */
	private boolean hears(Player them) {
		if (them.isSprinting() && this.distanceTo(them) < HEARS_SPRINT) {
			return true;
		}
		if (!(this.level() instanceof ServerLevel here)) {
			return false;
		}
		BlockPos noise = com.bloomlet.herobrine.manifest.Whereabouts.heard(here);
		return noise != null
			&& Math.sqrt(this.blockPosition().distSqr(noise)) < HEARS_DIGGING
			&& noise.closerThan(them.blockPosition(), 6.0);
	}

	/**
	 * IS HE ALREADY OUT? THERE IS ONLY EVER ONE OF HIM.
	 *
	 * Six places in this mod create a HerobrineEntity — the stare, the glimpse, the
	 * passage, the hunt, coming home, and now Whereabouts — and until he lived
	 * somewhere that was harmless, because each was a self-contained event that
	 * placed him, ran, and discarded him.
	 *
	 * It is not harmless now. He is a person with an address who is out walking, so
	 * a stare rolling while he is two hundred blocks away would put a SECOND one in
	 * front of the player. Two of him is not a scarier version of one of him; it is
	 * the end of him being anybody, and no amount of atmosphere recovers from a
	 * player seeing both at once.
	 *
	 * So every placement asks this first. The event is refused rather than
	 * redirected, which is the conservative half of the fix — the better version is
	 * for a stare to MOVE the one that exists instead of declining, and that is
	 * worth doing once this has been played.
	 */
	/**
	 * HOW FAR HE WILL STEP DOWN, AND IT IS NOT THE SAME IN BOTH MOODS.
	 *
	 * The node evaluator happily routes a three-block drop as a shortcut, and he
	 * can only ever step one back UP. So every ledge he took while wandering was a
	 * decision he could not undo: into a ravine, into a cave, into somebody's
	 * quarry, and then round and round the bottom of it for the rest of the
	 * evening. It is the single commonest way he got stuck and it never looked
	 * like being stuck — it looked like him choosing to be down there.
	 *
	 * One while prowling, so he only ever goes down what he can climb back out of.
	 * The default while hunting, because dropping after somebody is correct and
	 * getting back out is not his problem then.
	 */
	@Override
	public int getMaxFallDistance() {
		return this.present && !this.hunting ? 1 : super.getMaxFallDistance();
	}

	/**
	 * IS HE OUT IN THE OVERWORLD. NOT "does he exist".
	 *
	 * THE NAME WAS A TRAP AND IT CAUGHT ME. `anyLoaded(level)` reads as "is there
	 * one of him in this level", and its first line is a hard `return false` for
	 * every dimension that is not the overworld. Which was correct for the caller it
	 * was written for — a performance in the End should not count as him being out —
	 * and is a landmine for anybody who reads the signature and believes it.
	 *
	 * I used it as the guard on the placement over the keep. It returned false every
	 * time, once a second, for as long as anybody stood in his world. The playtest
	 * log is a wall of "hunt: going over" at one a second, each line a different
	 * entity taking off for the first time.
	 *
	 * So it keeps its behaviour and loses its misleading name. What the placement
	 * actually wanted is oneIn(level), below.
	 */
	public static boolean outInTheOverworld(ServerLevel level) {
		if (level.dimension() != net.minecraft.world.level.Level.OVERWORLD) {
			return false;
		}
		return oneIn(level) != null;
	}

	/**
	 * The one of him in THIS level, if there is one. No dimension opinion at all.
	 *
	 * @return him, or null — and returning the entity rather than a boolean is what
	 *         lets a caller that finds a second one do something about it
	 */
	public static @org.jspecify.annotations.Nullable HerobrineEntity oneIn(
			ServerLevel level) {
		for (HerobrineEntity him : all(level)) {
			return him;
		}
		return null;
	}

	/** Every one of him in this level, which should always be nought or one. */
	/**
	 * THE TYPE INDEX, NOT A BOX THE SIZE OF THE WORLD.
	 *
	 * This used to ask getEntitiesOfClass for everything inside a sixty-million
	 * block AABB, which is the obvious way to write "all of them" and is a spatial
	 * query over the entire loaded entity store. It reads as free because the box
	 * is a constant. It is not: the cost is every entity section a player has
	 * loaded, walked and type-checked, and on a server with a few people spread
	 * out that is thousands of entities per call.
	 *
	 * getEntities with an EntityTypeTest goes at the level's own index by type. It
	 * returns the same list and it never touches an entity that is not one of his.
	 * There is normally exactly ONE of him in a world and often none at all, so
	 * the honest cost of this question is nearly nothing — it was only expensive
	 * because of how it was being asked.
	 */
	@SuppressWarnings("unchecked")
	public static java.util.List<HerobrineEntity> all(ServerLevel level) {
		return (java.util.List<HerobrineEntity>) level.getEntities(
			net.minecraft.world.level.entity.EntityTypeTest.forClass(HerobrineEntity.class),
			him -> true);
	}

	public boolean isPresent() {
		return this.present;
	}

	/**
	 * Quartering the ground, and watching to see who looks back.
	 *
	 * @return true while he is still searching, false the moment it becomes a hunt
	 */
	private boolean prowl(java.util.List<Player> watchers) {
		// NOTHING OUT HERE KNOWS HOW TO FLY — except a duel, which owns his
		// movement outright while it runs and puts him down when it finishes.
		//
		// Landing unconditionally was too blunt: it confiscated the duel's flight
		// on any tick the duel had not yet claimed, which is what killed the
		// airborne lightning outside a hunt. The backstop is still here for
		// anything that leaves him up with nobody to fight, which is the actual
		// failure it exists for.
		if (this.flying && this.busyWith == null) {
			this.land();
		}
		this.listen();
		this.giveUpOnUnreachable();
		if (this.level() instanceof ServerLevel ground) {
			this.unwedge(ground);
		}
		// HE SEES THEM. THAT IS THE HUNT, AND IT IS THE WHOLE RULE.
		//
		// It used to be the other way round: the player had to hold HIM in the
		// middle of their screen for two and a half seconds. Which made the most
		// frightening thing in the mod something you did to yourself, and meant a
		// player who kept their eyes down was safe from him indefinitely.
		//
		// His eyes now. Come too close and get caught, and that is the only trigger
		// left anywhere — no phase gates it, no director schedules it, nothing is
		// owed. He is out here, and avoiding him is the game.
		//
		// A short lock rather than an instant one, so brushing past a gap in a
		// hedge at sprint is not a death sentence. Half a second of him actually
		// having you.
		// ALREADY COMING TO LOOK? Then that owns the tick and nothing else here runs.
		if (this.suspicion > 0) {
			return this.investigate(watchers);
		}

		Player who = null;
		for (Player watcher : watchers) {
			if (spots(watcher) || hears(watcher)) {
				who = watcher;
				break;
			}
		}
		this.eyeTicks = who != null ? this.eyeTicks + 1 : 0;

		if (this.eyeTicks >= LOCK_ON) {
			// NOT A HUNT. HE HAS NOTICED SOMETHING.
			//
			// Going straight from unaware to hunting made being caught a state
			// change rather than an event — no warning, nothing to react to, and
			// the player's only input was where they had happened to be standing.
			//
			// So he suspects first. He says something into the dark, turns, and
			// walks to the spot at a decent pace. If you are still near it when he
			// arrives, that is a hunt. If you are not, he stands there, says he
			// knows what he heard, and goes back to what he was doing.
			//
			// It is the same information as before — you have been noticed — handed
			// over a few seconds earlier, and those seconds are the entire game.
			this.eyeTicks = 0;
			this.suspicion = LOOKS_FOR;
			this.suspectAt = who.blockPosition();
			this.setSprinting(false);
			if (this.level() instanceof ServerLevel here) {
				com.bloomlet.herobrine.manifest.TheHunt.suspects(here, this.suspectAt, false);
			}
			return true;
		}

		// NOBODY IS ANYWHERE NEAR HIM ANY MORE, so he stops being an entity and goes
		// back to being a position. Not a despawn — the difference matters: his
		// whereabouts survive, he is still out there, and walking back into that
		// part of the world finds him again exactly where he had got to.
		if (this.level() instanceof ServerLevel out
			&& com.bloomlet.herobrine.manifest.Whereabouts.strayedOffScreen(out, this)) {
			HerobrineMod.LOGGER.debug("nobody near him — he carries on unwatched");
			this.discard();
			return false;
		}

		if (--this.prowlTicks <= 0) {
			// THE MINUTE NO LONGER STARTS ANYTHING. He is not on a schedule and
			// never was one: running out of prowl just means he keeps prowling.
			// Being SEEN is the only thing that starts a hunt now.
			this.prowlTicks = PROWL_TICKS;
			return true;
		}
		// A SHAFT BETWEEN HIM AND WHERE HE IS GOING IS A THING HE CLIMBS.
		//
		// Below the watching on purpose. Everything above this line is him noticing
		// people, and a man on a ladder still has eyes — an early return up there
		// would have made a rung the one place in the world you could walk past him.
		// It sits with the movement instead, and owns the tick when it fires,
		// because the navigator has no opinion about verticals and two hands on one
		// wheel is how most of the bugs in this file started.
		if (this.ladders(this.legTo)) {
			return true;
		}
		this.overIt();

		// The searching itself. Long walks, and his head pointed down them.
		//
		// It used to snap to a random bearing at the end of every leg, on the theory
		// that a head going somewhere other than the feet reads as somebody taking a
		// place in. It does not. It reads as a neck injury: nothing in the world
		// caused the turn, so there is nothing for a watcher to follow it to.
		//
		// A head is worth watching when it is ANSWERING something — see listen — and
		// the rest of the time it belongs pointed where he is walking, which is what
		// a person's head does and what the look control already does for free.
		if (--this.wanderIn <= 0) {
			this.wanderIn = WANDER_MIN + this.random.nextInt(WANDER_SPREAD);
			// TWENTY TO SEVENTY BLOCKS. Long enough that he is somewhere else by the
			// time he stops, and that a player who wants to follow him has to
			// actually keep up rather than stand and watch.
			boolean running = this.random.nextInt(4) == 0;
			this.setSprinting(running);
			double pace = running ? PROWL_SPRINT : PROWL_WALK;
			// TWO LEGS OUT, ONE LEG BACK. Without the return he wanders off after
			// the foundation and the house never gets a second course — and the
			// whole reason to follow him is that the thing he is building grows.
			BlockPos round;
			if (this.errand != null) {
				// SOMEWHERE TO BE. Out at a run, because he has decided; back at a
				// walk, because he is finished and nothing is chasing him.
				this.legTo = this.errand;
				this.setSprinting(!this.homeward);
				// moveTo returns false when there is no route at all — no reason to
				// spend three seconds proving it a second time.
				if (!this.getNavigation().moveTo(this.errand.getX() + 0.5,
						this.errand.getY(), this.errand.getZ() + 0.5,
						this.homeward ? PROWL_WALK : PROWL_SPRINT)) {
					HerobrineMod.LOGGER.info("prowl: nothing leads to [{}, {}, {}]",
						this.errand.getX(), this.errand.getY(), this.errand.getZ());
					this.errand = null;
					this.homeward = false;
					this.felling = 0;
					this.legTo = null;
				}
			} else if ((round = this.nextHaunt()) != null) {
				// The route is where the verticals are — two of its five stops are
				// down a shaft — so this is the leg ladders() most needs to see.
				this.legTo = round;
				this.getNavigation().moveTo(round.getX() + 0.5,
					round.getY(), round.getZ() + 0.5, pace);
			} else {
				double angle = this.random.nextDouble() * Math.PI * 2.0;
				double range = 20.0 + this.random.nextDouble() * 50.0;
				this.legTo = null;      // nowhere in particular, so nothing to climb for
				this.getNavigation().moveTo(
					this.getX() + Math.cos(angle) * range,
					this.getY(),
					this.getZ() + Math.sin(angle) * range, pace);
			}
		}

		if (--this.choreIn <= 0 && this.level() instanceof ServerLevel around) {
			this.choreIn = CHORE_MIN + this.random.nextInt(CHORE_SPREAD);
			this.chore(around);
		}
		return true;
	}

	/**
	 * WHAT HE IS DOING OUT HERE, AND IT IS NOT ALWAYS MASONRY.
	 *
	 * One chore and it was always the hut, so every prowl anybody watched was the
	 * same man laying the same five-by-five box, and a box is what he built because
	 * a box is the cheapest thing to build. It read as a mod placing blocks on a
	 * timer, which is exactly what it was.
	 *
	 * Four things now, rolled each time, each one refusing if there is nothing
	 * there for it — no animal, no tree, no hillside — and the build catching
	 * whatever falls through. So what he does is decided by WHERE HE IS, which is
	 * the whole difference between a routine and a person: he butchers in a field,
	 * he takes a tree apart at a treeline, he goes into a hill on a slope, and on
	 * open flat ground with nothing to hand, he builds.
	 */
	private void chore(ServerLevel around) {
		// AN ERRAND OWNS HIM UNTIL IT IS FINISHED. Rolling for something else while
		// he is halfway to a wood is how a person with somewhere to be turns back
		// into a mob wandering, and the whole value of the trip is that he does not
		// stop for anything on the way.
		if (this.errand != null || this.felling > 0) {
			this.errand(around);
			return;
		}
		// AND GOING OUT IS ONE OF THE ROLLS, NOT WHAT HAPPENS WHEN THE OTHERS FAIL.
		//
		// It used to be the fallback, which sounds modest and is not: butcher, fell
		// and burrow all refuse when there is no animal, no tree and no hillside,
		// and standing in a field that is every roll. So he set off for a wood every
		// few seconds and chained them without ever going home. A whole playtest log
		// of "sets off for the wood" and not one visit to his own house.
		//
		// One roll in six, and Whereabouts holds a four-minute floor under it that
		// survives him being discarded and rebuilt. The rest of the time he is doing
		// something where he is, or walking his route — which is what living
		// somewhere looks like.
		switch (this.random.nextInt(6)) {
			case 0, 1 -> {
				if (this.butcher(around)) {
					return;
				}
			}
			case 2 -> {
				if (this.fell(around)) {
					return;
				}
			}
			case 3 -> {
				if (this.burrow(around)) {
					return;
				}
			}
			case 4 -> {
				if (this.setOut(around)) {
					return;
				}
			}
			default -> { }
		}
	}

	/**
	 * WHAT COMES FOR HIM WITHOUT BEING ASKED.
	 *
	 * Golems and illagers had no opinion about him at all — he is not tagged as a
	 * monster, so nothing in vanilla's target lists could see him. Which meant the
	 * most obvious plan in Minecraft, "build a golem and stand behind it", did
	 * nothing whatsoever, and a pillager patrol would walk past him.
	 *
	 * Pointed at him directly instead, once a second, and only if they have nobody
	 * else in mind — a golem already defending its village keeps defending it.
	 *
	 * AND HE FIGHTS THEM RATHER THAN DELETING THEM. Everything that chose him used
	 * to stop on the tick it touched him, which is a good line and the wrong answer
	 * to this: a golem that evaporates is not a tactic, it is a lesson that tactics
	 * do not work. These three take a sword blow like anything else and can last
	 * long enough to matter. A hundred-health golem buys you real seconds.
	 */
	private static final double COMES_FOR_HIM = 20.0;
	/** Three quarters of a second between swings at anything that is not a player. */
	private static final int ANSWER_EVERY = 15;
	/** How high he works from, and how often a bolt comes down. */
	private static final double BOLT_FROM = 9.0;
	private static final int BOLT_EVERY = 40;
	/**
	 * AND HE CIRCLES IT RATHER THAN SITTING ON TOP OF IT.
	 *
	 * Holding station directly overhead was the easy version and it reads as a
	 * turret: a man pinned to a point in the sky, at a fixed height, throwing
	 * things straight down. Nothing about it looks like a decision.
	 *
	 * A ring instead, five to nine blocks out, at a height that breathes — and the
	 * direction and the radius are re-rolled every three to seven seconds, so he
	 * never completes the same lap twice and there is no pattern to shoot at. It
	 * is also a better angle: from directly above he is a dot, and from out on the
	 * ring he is a figure against the sky with the ground burning under him.
	 *
	 * Eased rather than teleported. He is placed at a point that moves smoothly,
	 * so the movement is the point moving and not him being re-placed.
	 */
	private static final double ORBIT_SPEED = 0.035;
	private static final double HOVER_PACE = 0.5;
	private double orbit;
	private double orbitWide = 7.0;
	private int orbitWay = 1;
	private int orbitFor;
	private int answerIn;

	/**
	 * SOMETHING IS HITTING HIM AND IT IS NOT YOU.
	 *
	 * Two things were wrong and they were the same thing. He never TURNED — the
	 * look control moves the head only, on purpose, so a hunt can keep the eyes on
	 * a player while the body walks somewhere else. Pointed at a golem that means
	 * a man facing you with his neck round, swinging sideways at something behind
	 * his shoulder.
	 *
	 * And he never NOTICED. The hunt locks to one player and nothing in it can be
	 * interrupted, so an iron golem could beat on him for a minute while he walked
	 * past it to get to you. Which threw away the best tactic in the game: build a
	 * golem, stand behind it, and buy yourself the seconds it survives.
	 *
	 * So a duel is a real interruption. Body, head and yaw all turned on the thing
	 * — he squares up — and the hunt is suspended, not cancelled: the moment the
	 * golem is dead or out of reach he goes straight back to you, mid-swing.
	 *
	 * Five seconds, refreshed every time either of them lands one, so a fight that
	 * is going on carries on and a golem that has wandered off is dropped.
	 */
	private static final int BUSY_FOR = 100;
	/**
	 * AND IT HAS TO BE FURTHER THAN THE RANGE HE PICKS THEM UP AT.
	 *
	 * answer() hands him a challenger from twelve blocks and this dropped one at
	 * nine — so anything between the two was adopted and abandoned on the same
	 * tick, every tick, for as long as it stood there. He never took a step toward
	 * a golem because the duel cancelled itself before it could ask him to.
	 *
	 * Sixteen, comfortably outside the pickup range, so the only thing that ends a
	 * duel is the thing dying, leaving, or five seconds of neither of them landing
	 * anything.
	 */
	private static final double BUSY_RANGE = 16.0;
	private net.minecraft.world.entity.@org.jspecify.annotations.Nullable Mob busyWith;
	private int busyFor;

	/**
	 * THREE OR MORE AND HE STOPS PICKING.
	 *
	 * One at a time is the right pace for one opponent and it is the wrong answer to
	 * a room. Against a crowd he was doing it correctly and losing anyway — a bolt
	 * every two seconds against six things is twelve seconds of them all hitting
	 * him, and every second of it looked like him being patient rather than him
	 * being outnumbered.
	 *
	 * So a crowd gets a different move. One turn on the spot and a bolt lands on
	 * every one of them — not simultaneously, which would be a flash and a pile of
	 * corpses, but RAKED: two ticks apart, so it travels across the room and you
	 * can watch it arrive at each of them in turn. That reading is the whole value.
	 * A single flash says a spell went off. Six bolts in order says he went down
	 * the line.
	 *
	 * ONLY WHAT CAME FOR HIM. Anything whose target is him, or one of the three
	 * that seek him out on their own. A cave full of zombies minding their own
	 * business is not a crowd, and Dread has them running anyway.
	 *
	 * Capped at eight, because thirty bolts inside a second is not a boss move, it
	 * is a tick spike — and Cadence has its own ceiling underneath that.
	 */
	private static final double SWEEP_REACH = 12.0;
	private static final int SWEEP_NEEDS = 3;
	private static final int SWEEP_MOST = 8;
	private static final int SWEEP_GAP = 2;
	private static final int SWEEP_EVERY = 140;
	private static final int SWEEP_LOOKS = 10;
	private int sweepIn;

	/** @return true if a sweep went out, and this tick belonged to it */
	private boolean sweep(ServerLevel field) {
		if (--this.sweepIn > 0) {
			return false;
		}
		java.util.List<net.minecraft.world.entity.Mob> lot =
			field.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
				this.getBoundingBox().inflate(SWEEP_REACH),
				m -> m.isAlive() && !(m instanceof HerobrineEntity)
					&& !com.bloomlet.herobrine.manifest.TheHunt.isHis(m)
					&& (challenger(m) || m.getTarget() == this));
		if (lot.size() < SWEEP_NEEDS) {
			// Half a second, not next tick. A twenty-four block box query every
			// tick for the whole of every duel is a real bill for an answer that
			// cannot change that fast.
			this.sweepIn = SWEEP_LOOKS;
			return false;
		}
		this.sweepIn = SWEEP_EVERY;
		this.swipe();
		int step = 0;
		for (net.minecraft.world.entity.Mob one : lot) {
			if (step >= SWEEP_MOST) {
				break;
			}
			// Held in a local so each scheduled bolt owns its own target — the loop
			// variable would be whatever the loop finished on by the time these run.
			net.minecraft.world.entity.Mob mark = one;
			com.bloomlet.herobrine.manifest.Cadence.in(field.getServer(),
				step * SWEEP_GAP, () -> {
					if (!this.isAlive() || !mark.isAlive()) {
						return;
					}
					com.bloomlet.herobrine.manifest.TheHunt.smite(field, mark);
					mark.hurtServer(field, this.damageSources().mobAttack(this),
						STRIKE_DAMAGE * 3.0F);
				});
			step++;
		}
		HerobrineMod.LOGGER.info("he went down the line — {} of them at once",
			Math.min(lot.size(), SWEEP_MOST));
		return true;
	}

	/** @return true if the duel took the tick — nothing else runs during one */
	private boolean duel() {
		net.minecraft.world.entity.Mob foe = this.busyWith;
		if (foe == null) {
			return false;
		}
		if (!foe.isAlive() || --this.busyFor <= 0
			|| this.distanceTo(foe) > BUSY_RANGE
			|| !(this.level() instanceof ServerLevel field)) {
			this.busyWith = null;
			// Whatever the duel put in the air, the duel takes out of it.
			if (this.flying && !this.hunting) {
				this.land();
			}
			return false;
		}
		// Nothing is stalling — there is a fight on, it is just not with them.
		this.stalemate = 0;

		// A ROOM OUTRANKS A DUEL. Above the flight and the sword both, because it
		// is the answer to there being more than one of them and neither of those is.
		if (this.sweep(field)) {
			this.squareUp(foe);
			return true;
		}

		// HE CROSSES THE GROUND THE SAME WAY HE CROSSES IT FOR A PLAYER.
		//
		// A bare moveTo was all this had, so anywhere the pathfinder came up empty
		// he simply stood and looked at the thing. closeOn has never had that
		// problem because it carries a fallback shove and the same legs the chase
		// uses, and there is no reason a golem should get a worse pursuer than you
		// do.
		//
		// And only the HEAD tracks while he is travelling. squareUp writes the body
		// yaw, which is the same field the move control steers with — turning his
		// shoulders every tick while walking fights the navigation for the wheel.
		// He squares up when he arrives, which is also when it matters.
		// FROM UP THERE HE THROWS LIGHTNING, BECAUSE HE HAS NOTHING ELSE.
		//
		// Flying was pure travel: a way over a wall, with no attack attached to it.
		// So anything with RANGE beat him for free — the Warden's sonic boom is
		// eleven blocks and does not care that he is airborne, and he had no answer
		// but to come down into it. He was being shot at while he circled.
		//
		// He holds station over the thing and calls bolts onto it. Slower than the
		// sword and it leaves craters, which is the trade: from up there he is
		// untouchable and inaccurate, and on the ground he is neither.
		if (this.flying) {
			this.setNoGravity(true);
			this.setDeltaMovement(Vec3.ZERO);
			// A new bearing every three to seven seconds, so the lap is never the
			// same lap and there is nothing to lead a shot on.
			if (--this.orbitFor <= 0) {
				this.orbitFor = 60 + this.random.nextInt(80);
				this.orbitWay = this.random.nextBoolean() ? 1 : -1;
				this.orbitWide = 5.0 + this.random.nextDouble() * 4.0;
			}
			this.orbit += this.orbitWay * ORBIT_SPEED;
			double want = foe.getY() + BOLT_FROM + Math.sin(this.age * 0.04) * 1.7;
			double toX = foe.getX() + Math.cos(this.orbit) * this.orbitWide;
			double toZ = foe.getZ() + Math.sin(this.orbit) * this.orbitWide;
			this.snapTo(
				this.getX() + net.minecraft.util.Mth.clamp(
					toX - this.getX(), -HOVER_PACE, HOVER_PACE),
				this.getY() + net.minecraft.util.Mth.clamp(
					want - this.getY(), -0.35, 0.35),
				this.getZ() + net.minecraft.util.Mth.clamp(
					toZ - this.getZ(), -HOVER_PACE, HOVER_PACE),
				this.getYRot(), 0.0F);
			this.squareUp(foe);
			if (this.answerIn <= 0) {
				this.answerIn = BOLT_EVERY;
				this.busyFor = BUSY_FOR;
				this.swipe();
				com.bloomlet.herobrine.manifest.TheHunt.smite(field, foe);
				foe.hurtServer(field, this.damageSources().mobAttack(this),
					STRIKE_DAMAGE * 2.0F);
			}
			return true;
		}
		if (this.distanceTo(foe) > ARMS_LENGTH) {
			this.getLookControl().setLookAt(foe, 90.0F, 90.0F);
			boolean routed = this.getNavigation().moveTo(foe, HUNT_SPEED);
			if (!routed || this.getNavigation().isDone()) {
				Vec3 step = new Vec3(foe.getX() - this.getX(), 0.0,
					foe.getZ() - this.getZ());
				if (step.lengthSqr() > 1.0E-4) {
					this.move(net.minecraft.world.entity.MoverType.SELF,
						step.normalize().scale(0.16));
				}
			}
			if (this.onGround() && !this.leap(foe.getX(), foe.getZ())) {
				int wall = this.wallAhead(foe.getX(), foe.getZ());
				if (wall > 0 && wall <= VAULT_MAX) {
					this.vault(foe.getX(), foe.getZ(), wall);
				}
			}
			// Nowhere to walk and still being shot at — then he goes up, where the
			// answer is lightning rather than reach.
			//
			// AND HE GOES UP FOR THIS WHETHER OR NOT HE IS HUNTING ANYBODY.
			//
			// I gated this on hunting to stop him hanging in the air, and it cost
			// the thing it was protecting: a Warden that turns up while nobody is
			// being hunted out-ranges him on the ground and he had no answer at
			// all. The freeze was never about hunting — it was that every land()
			// in this file takes a Player, so nothing could bring a prowling
			// Herobrine down.
			//
			// Fixed at the landing end instead: the duel puts him down when it
			// ends, and the prowl guard now only lands him when no duel owns him.
			if (this.getNavigation().isDone() && this.distanceTo(foe) > REACH + 3.0) {
				this.takeOff();
			}
			return true;
		}
		this.squareUp(foe);
		this.getNavigation().stop();
		if (this.answerIn <= 0) {
			this.answerIn = ANSWER_EVERY;
			this.busyFor = BUSY_FOR;
			this.swipe();
			this.doHurtTarget(field, foe);
		}
		return true;
	}

	/**
	 * FACING IT — body, yaw and head, all three.
	 *
	 * setLookAt alone is the head, which is deliberate everywhere else in this file
	 * and exactly wrong here. A man squaring up to something turns his shoulders to
	 * it, and the swing has to come off the front of him or it reads as a glitch.
	 */
	/** How fast the shoulders come round. A body is not a turret. */
	private static final float TURNS_AT = 18.0F;

	private void squareUp(net.minecraft.world.entity.Entity foe) {
		double dx = foe.getX() - this.getX();
		double dz = foe.getZ() - this.getZ();
		float yaw = (float) (net.minecraft.util.Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;

		// AND HE LOOKS DOWN AT IT, WHICH HE WAS NOT DOING.
		//
		// This only ever set yaw. Standing on the ground that is invisible — the
		// thing is at eye level anyway — and nine blocks above a Warden it is the
		// whole shot: he was throwing lightning at something under his feet while
		// staring out at the horizon.
		//
		// The look control would have handled pitch, but it runs inside super.tick
		// on the FOLLOWING tick and setYHeadRot below overwrites it every time. So
		// the pitch is worked out here, from the geometry, like the yaw is.
		double flat = Math.sqrt(dx * dx + dz * dz);
		double drop = this.getEyeY() - (foe.getY() + foe.getBbHeight() * 0.5);
		float pitch = (float) (net.minecraft.util.Mth.atan2(drop, flat) * (180.0 / Math.PI));
		this.setXRot(net.minecraft.util.Mth.clamp(pitch, -80.0F, 80.0F));
		this.setYHeadRot(yaw);

		// THE HEAD SNAPS, THE BODY TURNS.
		//
		// Slamming yBodyRot every tick is what made him a turret: the shoulders
		// arrived at the same instant as the eyes, from any angle, with no travel
		// in between. Eighteen degrees a tick is a quarter-second half-turn, which
		// reads as somebody rounding on a thing rather than being re-pointed at it.
		this.yBodyRot = net.minecraft.util.Mth.rotateIfNecessary(this.yBodyRot, yaw, TURNS_AT);
		this.setYRot(this.yBodyRot);
		this.getLookControl().setLookAt(foe, 90.0F, 90.0F);
	}

	/** Whatever just put a hand on him is now the thing he is dealing with. */
	private void nowDealWith(net.minecraft.world.entity.Entity what) {
		if (what instanceof net.minecraft.world.entity.Mob mob && mob.isAlive()) {
			this.busyWith = mob;
			this.busyFor = BUSY_FOR;
		}
	}

	public static boolean challenger(net.minecraft.world.entity.Entity what) {
		return what instanceof net.minecraft.world.entity.animal.golem.AbstractGolem
			|| what instanceof net.minecraft.world.entity.monster.illager.AbstractIllager
			// AND THE WARDEN, which is the one that settles it.
			//
			// It is the only thing in the game built to be unfightable — you are
			// meant to run from it, and every player knows that in their hands. So
			// it is the one opponent whose behaviour toward him the audience can
			// actually price: a Warden closing on Herobrine, and losing, is a
			// sentence no health bar could deliver.
			//
			// It also does not flee with the rest of the field. Nothing frightens
			// it, which is exactly why it has to be the thing that comes.
			|| what instanceof net.minecraft.world.entity.monster.warden.Warden;
	}

	private void provoke(ServerLevel field) {
		for (net.minecraft.world.entity.Mob mob : field.getEntitiesOfClass(
				net.minecraft.world.entity.Mob.class,
				this.getBoundingBox().inflate(COMES_FOR_HIM),
				m -> m.isAlive() && challenger(m)
					&& !com.bloomlet.herobrine.manifest.TheHunt.isHis(m))) {
			// EACH OF THEM BY THE MECHANISM IT ACTUALLY USES.
			//
			// setTarget works for a golem and does NOTHING to a Warden. Wardens do
			// not carry a target — they run on a brain, getTarget is overridden to
			// read a memory the brain rewrites every tick, and anything written
			// from outside is gone before the next one. It has its own anger
			// ledger and that is the only door in.
			//
			// A hundred and fifty is well past the threshold for furious, so it
			// arrives already committed rather than working its way up.
			if (mob instanceof net.minecraft.world.entity.monster.warden.Warden warden) {
				warden.increaseAngerAt(this, 150, true);
			} else if (mob.getTarget() == null) {
				mob.setTarget(this);
			}
			// AND HE DOES NOT WAIT TO BE TOLD.
			//
			// answer() only ever picked up something whose getTarget was already
			// him, which made the whole exchange depend on a field the Warden does
			// not have and the golem's own goals can overwrite. Proximity instead:
			// one of these three inside sixteen blocks is a fight, and whose
			// targeting system agreed to it first is not interesting.
			if (this.busyWith == null && this.distanceTo(mob) <= BUSY_RANGE) {
				this.nowDealWith(mob);
			}
		}
	}

	/** Sixteen blocks is far enough that he crosses a field for it. */
	private static final double BUTCHERS_AT = 16.0;
	/** And close enough to swing. */
	private static final double IN_REACH = 3.0;

	/**
	 * HE KILLS THINGS, AND HE DOES NOT EAT THEM.
	 *
	 * A cow standing in a field is the most ordinary thing in Minecraft and a cow
	 * lying dead in a field with nobody near it is not. Everything else he leaves
	 * behind is a block, which a player can tell themselves they forgot placing.
	 * They cannot tell themselves that about the animals.
	 *
	 * Nothing tamed. Somebody's dog is somebody's dog, and killing pets is a
	 * different mod.
	 */
	private boolean butcher(ServerLevel around) {
		net.minecraft.world.entity.animal.Animal beast = null;
		double best = Double.MAX_VALUE;
		for (net.minecraft.world.entity.animal.Animal candidate
				: around.getEntitiesOfClass(net.minecraft.world.entity.animal.Animal.class,
					this.getBoundingBox().inflate(BUTCHERS_AT),
					a -> a.isAlive()
						&& !(a instanceof net.minecraft.world.entity.TamableAnimal tame
							&& tame.isTame()))) {
			double away = this.distanceTo(candidate);
			if (away < best) {
				best = away;
				beast = candidate;
			}
		}
		if (beast == null) {
			return false;
		}
		this.getLookControl().setLookAt(beast, 90.0F, 90.0F);
		// Walks over first. The next chore finds it still standing there and
		// finishes it, which is a slower and much better thing to watch than a
		// cow falling over at range.
		if (best > IN_REACH) {
			this.getNavigation().moveTo(beast, PROWL_WALK);
			return true;
		}
		this.swipe();
		beast.hurtServer(around, this.damageSources().mobAttack(this), Float.MAX_VALUE);
		HerobrineMod.LOGGER.info("prowl: he killed a {} at [{}, {}, {}]",
			beast.getType().toShortString(),
			beast.getBlockX(), beast.getBlockY(), beast.getBlockZ());
		return true;
	}

	/**
	 * HE TAKES TWO OUT OF THE MIDDLE OF A TREE AND WALKS OFF.
	 *
	 * The floating canopy is the single most recognisable thing a person leaves in
	 * a Minecraft world, and no mob has ever made one. A player who comes across a
	 * trunk with a bite out of it and leaves hanging over it does not think
	 * "monster" — they think SOMEBODY WAS HERE, which is the entire point of him.
	 */
	private boolean fell(ServerLevel around) {
		BlockPos trunk = this.nearest(around, net.minecraft.tags.BlockTags.LOGS, 7);
		if (trunk == null) {
			return false;
		}
		this.getLookControl().setLookAt(Vec3.atCenterOf(trunk));
		if (Math.sqrt(this.blockPosition().distSqr(trunk)) > 4.0) {
			this.getNavigation().moveTo(trunk.getX() + 0.5, trunk.getY(),
				trunk.getZ() + 0.5, PROWL_WALK);
			return true;
		}
		this.swipe();
		int took = 0;
		int wants = 2 + this.random.nextInt(2);
		for (int up = 0; up < 4 && took < wants; up++) {
			BlockPos at = trunk.above(up);
			if (!around.getBlockState(at).is(net.minecraft.tags.BlockTags.LOGS)
				|| com.bloomlet.herobrine.manifest.DwellTracker.isBuilt(around, at)) {
				break;
			}
			around.destroyBlock(at, false);
			took++;
		}
		if (took == 0) {
			return false;
		}
		HerobrineMod.LOGGER.info("prowl: he took {} out of a tree at [{}, {}, {}]",
			took, trunk.getX(), trunk.getY(), trunk.getZ());
		return true;
	}

	/** How far into a hill he goes. Far enough to be dark at the back. */
	private static final int BURROW_DEEP = 7;

	/**
	 * HE GOES INTO HILLS.
	 *
	 * A one-by-two hole in a slope with a torch at the back of it, which is the
	 * first thing every Minecraft player has ever built and the last thing a mob
	 * would. It costs nothing, it is permanent, and it turns terrain the player
	 * already walked past into somewhere they now have to look into.
	 *
	 * Never through anything anybody placed — diggable refuses on built blocks, so
	 * he cannot tunnel into a base.
	 */
	private boolean burrow(ServerLevel around) {
		BlockPos feet = this.blockPosition();
		net.minecraft.core.Direction into = null;
		for (net.minecraft.core.Direction way
				: net.minecraft.core.Direction.Plane.HORIZONTAL) {
			BlockPos face = feet.relative(way);
			if (this.diggable(around, face) && this.diggable(around, face.above())
				&& around.getBlockState(face).isSolid()) {
				into = way;
				break;
			}
		}
		if (into == null) {
			return false;
		}
		this.getLookControl().setLookAt(Vec3.atCenterOf(feet.relative(into)));
		this.swipe();
		int deep = 3 + this.random.nextInt(BURROW_DEEP - 2);
		int cut = 0;
		int reached = 0;
		for (int step = 1; step <= deep; step++) {
			BlockPos at = feet.relative(into, step);
			boolean low = this.diggable(around, at);
			boolean high = this.diggable(around, at.above());
			if (!low && !high) {
				break;
			}
			if (low) {
				around.setBlock(at, Blocks.AIR.defaultBlockState(), 3);
				cut++;
			}
			if (high) {
				around.setBlock(at.above(), Blocks.AIR.defaultBlockState(), 3);
				cut++;
			}
			reached = step;
		}
		if (cut < 4) {
			return false;
		}
		BlockPos back = feet.relative(into, reached);
		if (around.getBlockState(back.below()).isSolid()) {
			this.put(around, back, Blocks.TORCH.defaultBlockState());
		}
		HerobrineMod.LOGGER.info("prowl: he went {} into the hill at [{}, {}, {}]",
			reached, back.getX(), back.getY(), back.getZ());
		return true;
	}

	/** The nearest block of a kind, searched outward from his feet. */
	private @org.jspecify.annotations.Nullable BlockPos nearest(ServerLevel around,
			net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> kind, int reach) {
		BlockPos feet = this.blockPosition();
		BlockPos found = null;
		double best = Double.MAX_VALUE;
		for (int dx = -reach; dx <= reach; dx++) {
			for (int dz = -reach; dz <= reach; dz++) {
				for (int dy = -2; dy <= 3; dy++) {
					BlockPos at = feet.offset(dx, dy, dz);
					if (!around.getBlockState(at).is(kind)) {
						continue;
					}
					double away = feet.distSqr(at);
					if (away < best) {
						best = away;
						found = at;
					}
				}
			}
		}
		return found;
	}

	/**
	 * HE ALREADY HAS A HOUSE. HE GOES OUT AND COMES BACK.
	 *
	 * The old chore laid a five-by-five hut in a field, then a seven-by-five one,
	 * and both were the wrong answer to the right question. He owns the most
	 * interesting property in the world — a homestead, a shed, forty metres of
	 * passage under both and a broken tower over the lot — and he was walking past
	 * all of it to put up a shack somewhere else. Somebody who builds a second
	 * house has not got a first one.
	 *
	 * So he does the two things a person with an address actually does. At home he
	 * walks his own land, on a route, in order — see nextHaunt. And every so often
	 * he goes OUT: picks a treeline fifty to a hundred and ten blocks off, sprints
	 * the whole way, and spends a minute taking it apart with an axe and setting
	 * what is left on fire.
	 *
	 * THE ERRAND IS THE PART A PLAYER FOLLOWS. Everything else he does happens
	 * where he is standing. This one has him leave at a run with somewhere to be,
	 * and the only way to find out where is to keep up — and keeping up with him is
	 * the single most dangerous thing in the mod, because it means being close.
	 *
	 * And what he leaves behind is a burnt hole in a wood, which is on the map for
	 * the rest of the save.
	 */
	private @org.jspecify.annotations.Nullable BlockPos errand;
	/** How many more chores he spends on the wood once he arrives. */
	private int felling;
	private static final int FELLING_FOR = 8;
	/** Near enough to have arrived. */
	private static final double AT_THE_TREELINE = 9.0;
	/** And how much height he will take on to get somewhere. He is walking. */
	private static final int CLIMBS = 14;

	/** Whether the thing he is walking to is his own front door. */
	private boolean homeward;

	/** @return true if he has somewhere to be, and this tick went to getting there */
	private boolean errand(ServerLevel around) {
		if (this.felling > 0) {
			return this.raze(around);
		}
		if (this.errand == null) {
			return false;
		}
		if (Math.sqrt(this.blockPosition().distSqr(this.errand)) > AT_THE_TREELINE) {
			return true;          // still walking. the leg picker is doing the work.
		}
		if (this.homeward) {
			HerobrineMod.LOGGER.info("prowl: he is back");
			this.errand = null;
			this.homeward = false;
			return false;
		}
		HerobrineMod.LOGGER.info("prowl: he got where he was going, [{}, {}, {}]",
			this.errand.getX(), this.errand.getY(), this.errand.getZ());
		this.errand = null;
		this.felling = FELLING_FOR;
		return this.raze(around);
	}

	/**
	 * HE LEAVES FROM HOME, AND NOT OFTEN.
	 *
	 * Both conditions matter. Starting anywhere turned the errand into a chain — he
	 * finished one wood and the next roll sent him to another, outward forever. And
	 * the four-minute floor lives in Whereabouts rather than on him, because he is
	 * discarded and rebuilt every time a player walks out of range and an
	 * entity-side cooldown would reset with him.
	 *
	 * @return true if he is now on his way somewhere
	 */
	private boolean setOut(ServerLevel around) {
		BlockPos house = com.bloomlet.herobrine.manifest.Whereabouts.home(around);
		if (house == null || !house.closerThan(this.blockPosition(), NEAR_HOME)
			|| !com.bloomlet.herobrine.manifest.Whereabouts.mayGoOut(around)) {
			return false;
		}
		BlockPos wood = this.treeline(around);
		if (wood == null) {
			return false;
		}
		com.bloomlet.herobrine.manifest.Whereabouts.wentOut(around);
		this.errand = wood;
		this.homeward = false;
		this.wasFrom = Double.MAX_VALUE;   // a new target is a new baseline
		this.setSprinting(true);
		this.wanderIn = 0;        // he leaves NOW, not at the end of this leg
		HerobrineMod.LOGGER.info("prowl: he sets off for the wood at [{}, {}, {}], {} blocks",
			wood.getX(), wood.getY(), wood.getZ(),
			(int) Math.sqrt(this.blockPosition().distSqr(wood)));
		return true;
	}

	/**
	 * Somewhere with trees on it, far enough off to be a journey.
	 *
	 * Sampled off the heightmap rather than searched — twenty-four columns, two
	 * lookups each, and a canopy is a big target so it finds one nearly always in a
	 * wood and nearly never in a desert. Which is correct: in a desert he has
	 * nothing to go and do, so he stays home.
	 */
	private @org.jspecify.annotations.Nullable BlockPos treeline(ServerLevel around) {
		BlockPos feet = this.blockPosition();
		for (int attempt = 0; attempt < 24; attempt++) {
			double angle = this.random.nextDouble() * Math.PI * 2.0;
			double range = 50.0 + this.random.nextDouble() * 60.0;
			int x = feet.getX() + (int) Math.round(Math.cos(angle) * range);
			int z = feet.getZ() + (int) Math.round(Math.sin(angle) * range);
			// Loaded chunks only. Generating terrain for a walk he may never take
			// is a hitch nobody asked for, and out there he stops being an entity
			// anyway — see Whereabouts.strayedOffScreen.
			if (!around.hasChunkAt(new BlockPos(x, around.getSeaLevel(), z))) {
				continue;
			}
			int top = around.getHeight(
				net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);
			net.minecraft.world.level.block.state.BlockState crown =
				around.getBlockState(new BlockPos(x, top - 1, z));
			if (!crown.is(net.minecraft.tags.BlockTags.LEAVES)
				&& !crown.is(net.minecraft.tags.BlockTags.LOGS)) {
				continue;
			}
			// topOf refuses to count logs and leaves as footing, so this is the
			// forest FLOOR under the canopy rather than the top of the tree.
			BlockPos floor = new BlockPos(x,
				com.bloomlet.herobrine.structure.Ground.topOf(around, x, z) + 1, z);
			// AND NOT UP A MOUNTAIN.
			//
			// Sampling by column says nothing about whether he can get there, and
			// the first playtest picked a wood twenty-five blocks above his head. He
			// walked at the cliff for a minute. A pine forest on a ridge is a real
			// treeline and a completely fake destination, so anything more than a
			// storey and a half of climb is somebody else's wood.
			if (Math.abs(floor.getY() - this.getBlockY()) > CLIMBS) {
				continue;
			}
			return floor;
		}
		return null;
	}

	/** How much of a tree comes out per pass, and how wide he will reach for one. */
	private static final int TAKES_UP_TO = 5;
	private static final int WOOD_REACH = 9;
	/** He does not burn his own doorstep. */
	private static final double NOT_NEAR_HOME = 45.0;

	/**
	 * TAKING A WOOD APART.
	 *
	 * An axe first — three to five logs out of the middle of a trunk, canopy left
	 * hanging — and then a fire in the stump, which vanilla spreads for him. He
	 * does not stay to watch it.
	 *
	 * NOTHING BURNS NEAR ANYBODY'S BUILD. The stump is only lit when a coarse sweep
	 * of the ground around it finds nothing a player placed, and never within
	 * forty-five blocks of his own house. Fire that eats somebody's base is not a
	 * scare, it is a bug report.
	 */
	private boolean raze(ServerLevel around) {
		if (--this.felling <= 0) {
			this.felling = 0;
			// AND HE GOES HOME. Left to the ordinary leg picker he would simply
			// wander on from wherever the wood was, which is a hundred blocks from
			// his house and getting further — the reason he was never in.
			BlockPos house = com.bloomlet.herobrine.manifest.Whereabouts.home(around);
			if (house != null) {
				this.errand = house;
				this.homeward = true;
				this.wasFrom = Double.MAX_VALUE;
				this.wanderIn = 0;
			}
			HerobrineMod.LOGGER.info("prowl: he leaves the wood and starts back");
			return false;
		}
		BlockPos trunk = this.nearest(around, net.minecraft.tags.BlockTags.LOGS, WOOD_REACH);
		if (trunk == null) {
			this.felling = 0;
			return false;
		}
		this.getLookControl().setLookAt(Vec3.atCenterOf(trunk));
		if (Math.sqrt(this.blockPosition().distSqr(trunk)) > 4.0) {
			this.getNavigation().moveTo(trunk.getX() + 0.5, trunk.getY(),
				trunk.getZ() + 0.5, PROWL_SPRINT);
			return true;
		}
		this.swipe();
		int took = 0;
		for (int up = 0; up < TAKES_UP_TO && took < 3 + this.random.nextInt(3); up++) {
			BlockPos at = trunk.above(up);
			if (!around.getBlockState(at).is(net.minecraft.tags.BlockTags.LOGS)
				|| com.bloomlet.herobrine.manifest.DwellTracker.isBuilt(around, at)) {
				break;
			}
			around.destroyBlock(at, false);
			took++;
		}
		if (took > 0 && this.random.nextInt(3) == 0 && this.nobodysLand(around, trunk)) {
			BlockPos stump = trunk.below();
			if (around.getBlockState(stump).isSolid()
				&& around.getBlockState(trunk).isAir()) {
				around.setBlock(trunk, Blocks.FIRE.defaultBlockState(), 3);
				HerobrineMod.LOGGER.info("prowl: he left a fire at [{}, {}, {}]",
					trunk.getX(), trunk.getY(), trunk.getZ());
			}
		}
		return true;
	}

	/** A coarse sweep — every third block over thirty — for anything anybody built. */
	private boolean nobodysLand(ServerLevel around, BlockPos at) {
		BlockPos house = com.bloomlet.herobrine.manifest.Whereabouts.home(around);
		if (house != null && house.closerThan(at, NOT_NEAR_HOME)) {
			return false;
		}
		for (int dx = -15; dx <= 15; dx += 3) {
			for (int dz = -15; dz <= 15; dz += 3) {
				for (int dy = -3; dy <= 6; dy += 3) {
					if (com.bloomlet.herobrine.manifest.DwellTracker.isBuilt(
							around, at.offset(dx, dy, dz))) {
						return false;
					}
				}
			}
		}
		return true;
	}

	/** Never over anything a player placed, and never through himself. */
	private void put(ServerLevel around, BlockPos at,
	                 net.minecraft.world.level.block.state.BlockState what) {
		if (com.bloomlet.herobrine.manifest.DwellTracker.isBuilt(around, at)) {
			return;
		}
		// HE STANDS INSIDE THE FOOTPRINT WHILE HE WORKS.
		//
		// AT_THE_SITE is seven blocks and the house is seven across, so he is
		// routinely standing exactly where the next course goes — and a wall going
		// up through his own chest is how he ended up bricked into his own build,
		// which no amount of pathfinding recovers from.
		//
		// The gap it leaves is not a defect. He is building a house he will never
		// finish; a course with a hole in it where somebody was standing is the
		// most honest thing on the whole structure.
		if (this.getBoundingBox().intersects(new net.minecraft.world.phys.AABB(at))) {
			return;
		}
		if (around.getBlockState(at).canBeReplaced() || diggable(around, at)) {
			around.setBlock(at, what, 3);
		}
	}

	/** Anything natural, so a course never eats bedrock or somebody's floor. */
	private boolean diggable(ServerLevel around, BlockPos at) {
		net.minecraft.world.level.block.state.BlockState was = around.getBlockState(at);
		return !was.isAir()
			&& was.getDestroySpeed(around, at) >= 0.0F
			&& !com.bloomlet.herobrine.manifest.DwellTracker.isBuilt(around, at);
	}


	/**
	 * A BAR, BECAUSE FORTY POINTS OF INVISIBLE PROGRESS IS NOT DIFFICULTY.
	 *
	 * There was no feedback of any kind. You could not tell whether the last minute
	 * of the fight had achieved anything, which means you could not make the only
	 * decision the fight actually asks — commit, or run. Fog is not the same as
	 * challenge, and every boss in this game has a bar for exactly this reason.
	 *
	 * White, because that is the only colour he has, and it darkens the sky at the
	 * edges — the one piece of vanilla boss furniture that reads as dread rather
	 * than as a health readout.
	 *
	 * It shows the number the fight is really keyed on: how far off driving him
	 * away you are, not the MAX_HEALTH attribute, which the hunt never touches.
	 */
	private final net.minecraft.server.level.ServerBossEvent bar = madeBar();


	private static net.minecraft.server.level.ServerBossEvent madeBar() {
		net.minecraft.server.level.ServerBossEvent made =
			new net.minecraft.server.level.ServerBossEvent(java.util.UUID.randomUUID(),
				net.minecraft.network.chat.Component.literal("Herobrine"),
				net.minecraft.world.BossEvent.BossBarColor.WHITE,
				net.minecraft.world.BossEvent.BossBarOverlay.PROGRESS);
		made.setDarkenScreen(true);
		return made;
	}

	private static final String[] ACT_NAMES = { "Herobrine", "Herobrine · II", "Herobrine · III" };
	private int barAct;

	private void showBar(java.util.List<Player> watchers) {
		if (this.bar == null) {
			return;
		}
		// THE BAR IS THE DUEL'S NOW. It was still wired to the hunt's damage ledger,
		// which nothing writes any more, so it sat at full for seventy blows and told
		// the one lie a boss bar must never tell. Blows taken of blows needed, and
		// the act in its name — he grows, and the bar says so first.
		if (!this.bound || !this.hisGround()) {
			if (!this.bar.getPlayers().isEmpty()) {
				this.bar.removeAllPlayers();
			}
			return;
		}
		int act = this.act();
		if (act != this.barAct) {
			this.barAct = act;
			this.bar.setName(net.minecraft.network.chat.Component.literal(
				ACT_NAMES[Math.min(ACT_NAMES.length - 1, act - 1)]));
		}
		this.bar.setProgress((float) net.minecraft.util.Mth.clamp(
			1.0 - (double) this.hits / this.blowsNeeded(), 0.0, 1.0));
		for (Player watcher : watchers) {
			if (watcher instanceof ServerPlayer near && !this.bar.getPlayers().contains(near)) {
				this.bar.addPlayer(near);
			}
		}
		for (ServerPlayer shown : java.util.List.copyOf(this.bar.getPlayers())) {
			if (!watchers.contains(shown)) {
				this.bar.removePlayer(shown);
			}
		}
	}


	private boolean hunting;
	/** Who he is after, so the box query stops being the thing that ends a hunt. */
	private @org.jspecify.annotations.Nullable UUID onThe;
	private int stuckTicks;
	private double lastDistance = Double.MAX_VALUE;

	/** How much he minds getting wet, which depends on why he is out. */
	private void wade(boolean freely) {
		float cost = freely ? 0.0F : 8.0F;
		this.setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.WATER, cost);
		this.setPathfindingMalus(
			net.minecraft.world.level.pathfinder.PathType.WATER_BORDER, cost);
	}


	// ---- HE TAKES WHAT YOU WERE CARRYING ----------------------------------
	//
	// DYING IS NOT THE END OF THE EVENT ANY MORE, AND IT WAS.
	//
	// The last living player in a hunt goes down, watchers drops them on the next
	// tick because it filters on isAlive, pickQuarry returns null, roundOver is
	// handed null and calls vanish. So the single outcome the whole thing had been
	// building towards was also the one that deleted him mid-frame, before the
	// player had finished reading their own death screen. And whatever they were
	// carrying stayed exactly where it fell, untouched, for the rest of the world.
	//
	// He killed them for it. So the kill is a BEAT: he walks over, takes it, stands
	// there a moment, and then looks up for whoever else is still here.


	// ---- END SPOILS -------------------------------------------------------


	/**
	 * How long he comes at you before breaking off.
	 *
	 * Shorter each time round, so the rhythm accelerates: a long first run that
	 * gives the player time to work out what is happening, then progressively
	 * less room to think in.
	 */
	private int chaseSpell() {
		return Math.max(70, 200 - this.breakOffs * 45) + this.random.nextInt(90);
	}

	// ---- END THE HUNT ----------------------------------------------------

	/**
	 * How long he stays once you have stopped looking.
	 *
	 * The single best moment available: the player breaks line of sight for a
	 * second — a tree, a corner, a glance at their hotbar — and when they look
	 * back the figure is gone. Nothing had to move while they were watching,
	 * which is what makes it impossible to argue with. Short enough that a
	 * quick glance away is enough, long enough that a flickering sightline
	 * through leaves does not fire it by accident.
	 */
	private static final int UNSEEN_GRACE = 16;

	/**
	 * Two cones, because "on screen" and "being looked at" are different
	 * questions and sharing one answer between them broke the timer.
	 *
	 * SEEN_CONE is wide — over a hundred degrees — and decides whether the
	 * visit counted and whether everybody has lost him.
	 *
	 * HELD_CONE is narrow, about twenty degrees, and is the only thing that
	 * runs the countdown. He is near the middle of your view and you have him,
	 * which is what the allowance was always meant to be measuring.
	 */
	private static final double SEEN_CONE = 0.55;
	private static final double HELD_CONE = 0.93;

	/**
	 * Faster than a sprinting player, and by a margin.
	 *
	 * A sprint is about 0.28 blocks a tick, so the old 0.34 opened the gap at a
	 * walking pace and a determined player stayed on him the whole way. Being
	 * ALMOST able to catch him is the worst possible outcome: it makes him a
	 * mob with a speed stat rather than something that leaves when it chooses.
	 */
	private static final double FLEE_SPEED = 0.52;
	/** He does not run for long. He runs until he is out of sight. */
	private static final int FLEE_LIMIT = 70;

	/** How far out he cares who is watching. */
	/**
	 * How far away somebody still counts as present.
	 *
	 * NINETY-SIX, and it has to be bigger than the furthest the spawner will
	 * ever put him. It was 64 while HauntingSpawner.MAX_RADIUS was 68, so a
	 * placement out at the far end had NOBODY inside its own watcher box and
	 * hit the `watchers.isEmpty()` branch on its very first tick — placed and
	 * discarded before a single packet reached anyone. Two numbers that had to
	 * agree and did not, which is the shape of most of the bugs in this repo.
	 */
	private static final double WATCH_RANGE = 96.0;

	private static final int DEFIANCE_STRUCK = 40;
	/**
	 * What surviving a hunt costs you, and it is the largest number here.
	 *
	 * This is the whole engine of the mod stated in one constant. You cannot
	 * kill him, so the only thing you can do to a hunt is outlast it — and
	 * outlasting it is the loudest defiance available, so it brings him on.
	 * HUNTER is a thousand and SIEGE is eighteen hundred, which is six or seven
	 * survived hunts: enough that the ladder is felt rather than climbed in an
	 * evening.
	 *
	 * Enduring it is worth more than slipping it. A player who hid in a hole
	 * until he lost interest has done something cleverer and less defiant than
	 * one who was reached three times and was still standing, and the numbers
	 * should say which of those he minds more.
	 */
	// ---- THE RECKONING ----------------------------------------------------
	/**
	 * How many blows it takes, and why it is counted in blows.
	 *
	 * Damage would make this fight a different length for every player: a
	 * netherite axe would end it in four swings and a stone sword would take
	 * thirty, and every scripted beat in between would land in the wrong place
	 * or not at all. Counting hits means the fight has the SHAPE it was written
	 * with — the tenth blow is the tenth blow for everybody.
	 *
	 * It also removes the incentive to spend an hour on gear before starting.
	 * What decides this is whether the player can survive thirty exchanges,
	 * which is a question about them rather than about their inventory.
	 *
	 * Ten is the marker, not the total. Three acts of ten: he gets angrier, then
	 * the church arrives and tells them what they have done, then it gets much
	 * worse.
	 */
	private static final int TOTAL_HITS = 30;
	public static final int THE_WARNING = 10;

	private int hits;

	/**
	 * Which act the fight is in, one to three.
	 *
	 * The acts exist so the fight ESCALATES rather than simply lasting. Thirty
	 * exchanges of the same exchange is a health bar with extra steps; thirty
	 * that change twice is a fight the player can describe afterwards, and they
	 * will describe it by its turning points.
	 *
	 * One is the standoff they already know from the hunt, only now he does not
	 * leave. Two opens at the church, when he stops fighting the way a man
	 * would. Three is everything at once.
	 */
	/**
	 * THE ACTS ARE THIRDS OF THE FIGHT, not the first thirty blows of it.
	 *
	 * This divided by THE_WARNING, a constant ten — which was the same thing as a
	 * third only because blowsToKill happened to be thirty. Raise the health and
	 * the structure came apart: act three would start a third of the way in and
	 * run for the remaining two thirds, so the ending arrived early and then went
	 * on for twice as long as everything before it.
	 *
	 * Derived now, so the shape survives any number somebody puts in the config.
	 */
	private int act() {
		return Math.min(3, 1 + this.hits / Math.max(1, this.blowsNeeded() / 3));
	}

	/**
	 * THE FIGHT IS LONGER FOR A GROUP, AND IT NEVER GETS SHORTER MID-WAY.
	 *
	 * Seventy blows is seventy blows: four people land them in a quarter of the
	 * time one does, and a fight tuned to be five minutes alone was seventy
	 * seconds for a party. So the count scales with the PEAK party — the most
	 * distinct players who have struck him inside a minute of each other — by half
	 * again per extra hand, capped at three times. Peak, not current, so a player
	 * dying or leaving never moves the acts backwards; and kept on the level with
	 * the count, so a save does not shrink the fight either.
	 *
	 *     1 player    70      3 players   140
	 *     2 players  105      4+ players  175 .. 210 (cap)
	 */
	private static final long PARTY_WINDOW = 1200L;
	private static final double PER_EXTRA_HAND = 0.5;
	private static final double PARTY_CAP = 3.0;
	private final java.util.Map<java.util.UUID, Long> strikers = new java.util.HashMap<>();
	private int partyPeak = 1;

	private int blowsNeeded() {
		double scale = Math.min(PARTY_CAP, 1.0 + PER_EXTRA_HAND * (this.partyPeak - 1));
		return Math.max(1, (int) Math.round(Config.get().blowsToKill * scale));
	}

	private void noteStriker(ServerPlayer striker, long now) {
		this.strikers.put(striker.getUUID(), now);
		this.strikers.values().removeIf(at -> now - at > PARTY_WINDOW);
		if (this.strikers.size() > this.partyPeak) {
			this.partyPeak = this.strikers.size();
			HerobrineMod.LOGGER.info("duel: {} of them now — {} blows to the end",
				this.partyPeak, this.blowsNeeded());
		}
	}

	/**
	 * HOW BIG HE IS IN EACH ACT, AND IT IS THE WHOLE POINT OF HAVING ACTS.
	 *
	 * The fight has had three of them since it was written and every one looked
	 * identical. Act two starts throwing fireballs and calling lightning, act three
	 * throws three at once and spends half its time off the ground — real
	 * escalation, entirely invisible, so what a player experiences is one long
	 * fight that gets vaguely harder for no stated reason.
	 *
	 * Size is the cheapest sentence a boss can say. SCALE carries the hitbox with
	 * it, so a bigger him is bigger to hit as well as bigger to look at, and
	 * nothing about the model or the animation changes: SAME MAN, WRONG SIZE, which
	 * is worth more than any new mesh.
	 *
	 * 1.4 AND 1.7, NOT TWO. Two puts him at three and a half blocks, and the
	 * Reckoning can happen anywhere — including inside somebody's house, where a
	 * boss who cannot fit through his own doorway stops being frightening and
	 * becomes a physics problem. 1.7 is a foot over two blocks: unmistakable next
	 * to a player, and still able to get through a door.
	 */
	private static final double[] ACT_SIZE = { 1.0, 1.4, 1.7 };

	/**
	 * Applied on every blow, because that is the only moment the act can change.
	 *
	 * Set on the attribute rather than tracked in a field so the client gets it for
	 * free — SCALE is synced, and the renderer reads the size back off the render
	 * state to choose which face to draw. One number does both jobs and they cannot
	 * come apart.
	 */
	private void wearTheAct() {
		net.minecraft.world.entity.ai.attributes.AttributeInstance size =
			this.getAttribute(Attributes.SCALE);
		if (size == null) {
			return;
		}
		double want = ACT_SIZE[Math.min(ACT_SIZE.length - 1, this.act() - 1)];
		if (size.getBaseValue() == want) {
			return;
		}
		size.setBaseValue(want);
		if (this.level() instanceof ServerLevel here) {
			// He grows where you can hear it.
			here.playSound(null, this.getX(), this.getY(), this.getZ(),
				SoundEvents.WARDEN_ROAR, this.getSoundSource(), 2.4F, 0.5F);
			this.scorch(here, 6);
			this.ascend(here);
		}
		HerobrineMod.LOGGER.info("act {} — he stands {}x now", this.act(), want);
	}

	/** How high he goes, how long he hangs there, and how wide the ring is. */
	private static final int RISES = 10;
	private static final int HANGS_FOR = 60;
	private static final double RING = 4.5;

	/** Ticks left of the pause. Nothing he does runs while this is above zero. */
	private int ascending;

	public boolean isAscending() {
		return this.ascending > 0;
	}

	/**
	 * THE PAUSE BEFORE THE VIOLENCE, and the fight had none.
	 *
	 * He grew, roared, scorched the ground and carried straight on swinging, so the
	 * three acts were a number going up rather than three things happening. A fight
	 * with no breath in it is noise — the same fault the books had, and the same
	 * fix: stop, and let the thing that just happened land.
	 *
	 * So at every change he breaks off, climbs ten blocks, and HANGS THERE FOR
	 * THREE SECONDS doing nothing at all. No fireballs, no arrows, no lightning at
	 * the player. He burns, the eyes are the only light in the fog, and the bolts
	 * come down in a ring around HIM.
	 *
	 * THAT IS WHY THE RING IS ROUND HIM AND NOT ROUND YOU. Every other bolt in this
	 * fight is thrown at the player and is a threat; these are not aimed at
	 * anybody, they are what he looks like now. Visual-only for the same reason —
	 * the one moment the player is meant to stand still and look is the one moment
	 * nothing should be punishing them for it.
	 *
	 * And it is three seconds of free hits. That is deliberate and it is the trade:
	 * he buys the spectacle by handing the player the only opening in the fight.
	 */
	private void ascend(ServerLevel here) {
		this.ascending = HANGS_FOR;
		this.takeOff();
		this.setDeltaMovement(0.0, 0.0, 0.0);
		this.snapTo(this.getX(), this.getY() + RISES, this.getZ(),
			this.getYRot(), 0.0F);
		this.hurtMarked = true;
		this.setRemainingFireTicks(HANGS_FOR + 40);
		here.playSound(null, this.getX(), this.getY(), this.getZ(),
			SoundEvents.LIGHTNING_BOLT_THUNDER, this.getSoundSource(), 3.0F, 0.6F);

		int bolts = 6;
		for (int i = 0; i < bolts; i++) {
			double angle = Math.PI * 2.0 * i / bolts
				+ this.random.nextDouble() * 0.4;
			final double x = this.getX() + Math.cos(angle) * RING;
			final double z = this.getZ() + Math.sin(angle) * RING;
			com.bloomlet.herobrine.manifest.Cadence.in(here.getServer(),
					6 + i * 7, () -> {
				int y = here.getHeight(
					net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
					net.minecraft.util.Mth.floor(x), net.minecraft.util.Mth.floor(z));
				net.minecraft.world.entity.LightningBolt bolt =
					net.minecraft.world.entity.EntityTypes.LIGHTNING_BOLT
						.create(here, net.minecraft.world.entity.EntitySpawnReason.EVENT);
				if (bolt == null) {
					return;
				}
				bolt.setVisualOnly(true);
				bolt.snapTo(x, y, z, 0.0F, 0.0F);
				here.addFreshEntity(bolt);
			});
		}
		HerobrineMod.LOGGER.info("he is hanging over it, on fire, for {} ticks",
			HANGS_FOR);
	}
	// ---- END THE RECKONING ------------------------------------------------

	private static final int DEFIANCE_ENDURED = 130;

	/**
	 * How many times a chase relocates him before he actually goes.
	 *
	 * Bounded so a player cannot herd him around indefinitely — by the third
	 * approach the trick would be a mechanic rather than a fright.
	 */
	private static final int MAX_RELOCATIONS = 2;

	/**
	 * How often his arrival makes a sound, one in N.
	 *
	 * Not always, on purpose. A reliable cue becomes a tell — players learn
	 * "noise means he is behind me" and it stops being a fright and starts
	 * being a mechanic. At one in three you cannot trust it to mean anything,
	 * so it never resolves into a signal.
	 */
	private static final int CUE_CHANCE = 3;


	/**
	 * One sound as he arrives, sometimes — a reason to turn around.
	 *
	 * Uses the step sound of whatever he is standing on: grass outdoors, stone
	 * in a cave, gravel on a beach. It always suits the surroundings, so it
	 * reads as the world making an ordinary noise rather than as a mod cue,
	 * and it can never be learned as one specific "Herobrine sound".
	 *
	 * That ordinariness is the point. Your brain files it as an animal and you
	 * turn round casually — and he is there. Being casually wrong is worse
	 * than being forewarned, which is why this is a rustle and not a stick
	 * snapping or a block being placed. Those are sounds only a person can
	 * make, so they alarm you before you have even turned.
	 */
	public void announceArrival() {
		if (!(this.level() instanceof ServerLevel server)
			|| this.random.nextInt(CUE_CHANCE) != 0) {
			return;
		}
		BlockPos below = this.blockPosition().below();
		SoundEvent step = server.getBlockState(below).getSoundType().getStepSound();
		server.playSound(null, this.getX(), this.getY(), this.getZ(),
			step, this.getSoundSource(), 0.5F, 0.9F + this.random.nextFloat() * 0.1F);
	}

	/**
	 * The head does not obey the neck.
	 *
	 * Seventy-five degrees is the vanilla limit and it is a limit about
	 * anatomy — past it, a mob's head snaps back to the body. That is correct
	 * for a cow and wrong for this: the image the whole hunt is built around is
	 * a figure walking one way with its face still pointed at you, and at
	 * seventy-five it gives up and looks where it is going like anything else.
	 *
	 * A hundred and fifty is well past where a neck stops. It is meant to be.
	 */
	@Override
	public int getMaxHeadYRot() {
		return 150;
	}

	/**
	 * And it tracks fast enough to stay there.
	 *
	 * Ten degrees a tick is a slow, natural swivel that visibly lags behind a
	 * player circling him, which reads as him losing track. Forty keeps him
	 * locked on through anything short of a sprint around him.
	 */
	@Override
	public int getHeadRotSpeed() {
		return 40;
	}

	public HerobrineEntity(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, TOTAL_HITS)
			// createMobAttributes does NOT include ATTACK_DAMAGE — it is
			// LivingEntity's set plus FOLLOW_RANGE and nothing else — so
			// doHurtTarget would have read the bare default and swung for
			// nothing. Monsters get this from createMonsterAttributes; he does
			// not extend Monster, so he has to ask for it.
			.add(Attributes.ATTACK_DAMAGE, STRIKE_DAMAGE)
			// Registered so wearTheAct has something to set. Starts at a man's size.
			.add(Attributes.SCALE, 1.0)
			.add(Attributes.ATTACK_KNOCKBACK, 0.6)
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			// He needs to be aware of you from much further than he ever
			// approaches — the whole behaviour is about distance.
			.add(Attributes.FOLLOW_RANGE, 96.0)
			// He swims like he walks.
			//
			// travelInWater accelerates at a flat 0.02 unless this attribute
			// says otherwise, which is why an unmodified mob crossing a river
			// looks like it is wading through setting concrete. At 1.0 the
			// acceleration term becomes his actual movement speed — the same
			// mechanism Depth Strider uses — so a lake stops being a moat.
			.add(Attributes.WATER_MOVEMENT_EFFICIENCY, 1.0)
			// A full block, and then some, taken in his stride.
			//
			// Vanilla is 0.6, which is a slab — so a single block of terrain
			// made him stop and jump like anything else, and a fence line or a
			// stepped hillside broke the walk into a series of hops. At 1.6 he
			// comes up a block without altering his pace at all, which is much
			// worse to watch than a thing that has to climb.
			.add(Attributes.STEP_HEIGHT, 1.6);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.blade();
		// Nothing he carries is ever left on the ground. He does not die — he
		// is invulnerable and discards himself — but a guaranteed-drop slot
		// would hand a player a free diamond axe the first time anything else
		// managed to remove him.
		this.setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND, 0.0F);
		// Water does not stop him.
		//
		// Ground navigation treats it as something to path AROUND, so a river
		// between him and the player turned a pursuit into a figure jogging up
		// and down the bank. Both halves are needed: setCanFloat keeps him
		// swimming instead of sinking, and zeroing the malus stops the path
		// finder pricing water as a thing to avoid in the first place.
		this.getNavigation().setCanFloat(true);
		// AND HE OPENS DOORS, WHICH TOOK THREE LINES AND ONLY EVER HAD ONE.
		//
		// setCanOpenDoors is a PATHFINDER flag. It tells the route planner a closed
		// wooden door is not a wall — and that was all that was here, so he
		// cheerfully planned a path through his own front door and then stood
		// against it, because nothing in the entity had any idea how to turn a
		// handle. He has been locked out of his own house since it was built.
		//
		// canPassDoors makes the evaluator actually score the doorway, and
		// OpenDoorGoal is the vanilla machinery that does the opening.
		//
		// FALSE, so he never shuts it behind him. A door standing open that you
		// closed is the oldest thing in this story and it costs nothing to leave.
		this.getNavigation().setCanOpenDoors(true);
		this.getNavigation().getNodeEvaluator().setCanPassDoors(true);
		this.goalSelector.addGoal(1,
			new net.minecraft.world.entity.ai.goal.OpenDoorGoal(this, false));
		// WATER IS FREE TO A HUNT AND EXPENSIVE TO A WALK.
		//
		// Zeroing the malus outright was right for a chase — a river between him
		// and the player turned a pursuit into a figure jogging up and down the
		// bank. It was wrong for every other minute of the game: a wandering man
		// walked into every stream he met, and flowing water pushes harder than
		// navigation does, so he went downriver and did not come back.
		//
		// Set per state in beginHunt and beginProwl instead. He fords it when it
		// matters and goes round it when it does not, which is what a person does.
		this.wade(false);

		// HOW HARD HE IS ALLOWED TO THINK ABOUT IT.
		//
		// A* stops after a fixed number of visited nodes, and the default budget is
		// tuned for a zombie shuffling at a fence. It is why every mob in this game
		// gives up on anything with a corner in it: not because there is no route,
		// but because it ran out of patience three rooms before it found one.
		//
		// Four times the budget is the single largest thing that separates him from
		// a mob. It is what lets him solve a cellar, a switchback, the inside of
		// somebody's base, and the far side of a ravine — geometry that is entirely
		// routable and that nothing in vanilla will ever route.
		//
		// It is also cheap where it matters: the cost is only paid when a path is
		// actually requested, which for him is once every few seconds.
		this.getNavigation().setMaxVisitedNodesMultiplier(4.0F);

		// AND NOTHING THAT MERELY HURTS IS A WALL.
		//
		// The whole malus table exists to keep mobs alive. He cannot be hurt — he
		// is invulnerable and removes himself — so every one of these is the
		// pathfinder protecting somebody who does not need protecting, at the cost
		// of him walking round a campfire like a sheep.
		//
		// Fire is free, and a figure crossing a burning field without altering his
		// pace is worth more than any animation in the mod. Lava stays expensive
		// rather than free: he will wade a flow that is genuinely in the way and he
		// will not treat a lava lake as a shortcut.
		for (net.minecraft.world.level.pathfinder.PathType hurts : new
				net.minecraft.world.level.pathfinder.PathType[] {
			net.minecraft.world.level.pathfinder.PathType.FIRE,
			net.minecraft.world.level.pathfinder.PathType.FIRE_IN_NEIGHBOR,
			net.minecraft.world.level.pathfinder.PathType.DAMAGING,
			net.minecraft.world.level.pathfinder.PathType.DAMAGING_IN_NEIGHBOR,
			net.minecraft.world.level.pathfinder.PathType.DAMAGE_CAUTIOUS,
			net.minecraft.world.level.pathfinder.PathType.STICKY_HONEY,
			net.minecraft.world.level.pathfinder.PathType.COCOA,
			net.minecraft.world.level.pathfinder.PathType.TRAPDOOR,
			net.minecraft.world.level.pathfinder.PathType.DOOR_OPEN,
		}) {
			this.setPathfindingMalus(hurts, 0.0F);
		}
		this.setPathfindingMalus(
			net.minecraft.world.level.pathfinder.PathType.POWDER_SNOW, 8.0F);
		this.setPathfindingMalus(
			net.minecraft.world.level.pathfinder.PathType.LAVA, 12.0F);
		// No approach goal, deliberately. He used to close to a standoff
		// distance, which meant the player watched him WALK, and something you
		// watch cross a field is something you are studying rather than
		// something you have caught sight of. He is placed where he is placed
		// and he stays there: the whole event is a figure at a distance that
		// was already standing there when you looked up.
		// No LookAtPlayerGoal. Facing the player is not something to leave to a
		// probability, so tick() drives the rotation directly — and two things
		// both writing yaw is how most of the bugs in this repo started.
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) {
			return;
		}
		if (this.isDying() && this.level() instanceof ServerLevel leaving) {
			this.dyingTick(leaving);
			return;
		}

		// THE PAUSE HOLDS HIM WHERE HE IS. See ascend().
		//
		// Ticked before anything else so nothing downstream sees a stale count, and
		// the velocity is zeroed every tick rather than once: he is flying, and a
		// flying mob that is merely told to stop drifts. Hanging perfectly still is
		// the entire image.
		if (this.ascending > 0) {
			this.ascending--;
			this.setDeltaMovement(0.0, 0.0, 0.0);
			this.setNoGravity(true);
			if (this.ascending == 0) {
				this.setNoGravity(false);
				HerobrineMod.LOGGER.info("he is coming down");
			}
		}

		// ONE increment, then decide. Written as two separate `++this.age`
		// checks first, which aged him twice a tick whenever a fixed lifetime
		// was set — so every glimpse ran for half as long as it said, and the
		// shortest of them were four ticks. A fifth of a second is not a
		// glimpse, it is a rendering error.
		// AND THE BLADE IS PUT IN HIS HAND WITH A LIVE LEVEL UNDER HIM.
		//
		// blade() runs from registerGoals, which is the constructor — and the
		// enchantments are read out of the level's registry, behind an
		// `instanceof ServerLevel` guard. If that guard is ever false at
		// construction time the sword is still equipped and every enchantment on
		// it is silently missing: no fire, no knockback, no glint, and nothing
		// anywhere to say so.
		//
		// Once, on the first server tick, where the level is unambiguous.
		if (!this.armed) {
			this.armed = true;
			this.blade();
		}
		this.age++;
		// A SET PIECE OWNS EVERY TICK OF ITSELF. See beginShowing.
		if (this.showing) {
			return;
		}
		if (this.provokedFor > 0) {
			this.provokedFor--;
		}

		// NOTHING ALIVE STAYS ALIVE IN ARM'S REACH OF HIM.
		//
		// The field he crosses on the way to you should look like he crossed it.
		// Cheap to say and it says a great deal: you come out afterwards and the
		// two cows by the fence are dead where they stood, which is worse than
		// being told anything, because nobody told you.
		//
		// ARM'S REACH, NOT A RADIUS. Three blocks is the difference between "it
		// got in his way" and "he emptied my farm from across the paddock" — the
		// second is not menace, it is somebody's afternoon deleted, and it would
		// have made the pen next to a base the single worst place to build. He is
		// not hunting them; they are simply in the way of something that does not
		// go around.
		//
		// Every half second rather than every tick, so it is one small query and
		// not sixty.
		if (this.answerIn > 0) {
			this.answerIn--;
		}
		if (this.age % CULL_EVERY == 0 && this.level() instanceof ServerLevel field) {
			this.provoke(field);
			this.answer(field);
		}

		// A fixed lifetime outranks everything. These are not short stares —
		// they are different events that happen to use the same entity, and
		// none of the stare's rules about being looked at apply to them.
		if (this.glimpseTicks > 0 && this.age > this.glimpseTicks) {
			this.vanish("its time was up");
			return;
		}

		// Gone on his own, before he can become furniture. A stare only.
		// NOT WHILE HE IS STILL LOOKING FOR THEM.
		//
		// LIFETIME is thirty seconds and a prowl is sixty, so this would have
		// deleted him half way through every single one — and the symptom would have
		// been the hunt never happening at all, for no reason anybody could see,
		// about half the time. The stare's clock has no business running on a state
		// that has its own.
		//
		// AND THE OPENING IS THE THIRD ONE, WHICH COST A WHOLE FEATURE.
		//
		// investigate catches somebody, sets prowling false and starts the opening —
		// and by then he has been out walking for minutes, so age is far past
		// LIFETIME. This line ran on the very next tick and deleted him before
		// openOn had run once. Whereabouts then materialised a fresh one, which
		// began prowling, so the log read "he came to look and Robin was still
		// there" followed immediately by "aged out" and a new prowl.
		//
		// The hunt never started. Not sometimes — never, from the moment the
		// suspicion beat went in, because the beat is what sets prowling false.
		//
		// Same shape as every other ordering bug in this file: a guard that lists
		// the states it tolerates, and a new state nobody added to the list.
		// A STARE OR A GLIMPSE, WHICH NEVER CALLED beginProwl AND SO IS NOT PRESENT.
		// The wanderer is exempt by construction now rather than by remembering to
		// list him: he has no lifetime because he is not an event.
		if (!this.present
			&& this.age > LIFETIME) {
			this.vanish("aged out, nobody ever turned round");
			return;
		}


		// Everyone, not just whoever happens to be closest.
		//
		// Nearest-player-only was wrong in every direction the moment a second
		// person was in the world. A friend sprinting at him from behind while
		// you stood still did nothing; you looking away made him leave even
		// though your friend was staring straight at him; and fleeing from the
		// nearest player ran him directly into the other one.
		List<Player> watchers = this.level().getEntitiesOfClass(
			Player.class, this.getBoundingBox().inflate(WATCH_RANGE),
			player -> player.isAlive() && !player.isSpectator());

		// Did ANYONE actually see him? Not "was he rendered" — was he in
		// somebody's view, unobstructed. A visit nobody perceived should not
		// count against the pacing budget (see ManifestationDirector).
		boolean seen = false;
		// And separately: has anybody actually got him, rather than merely
		// having him somewhere on their screen? Only that runs the countdown.
		boolean held = false;
		for (Player watcher : watchers) {
			if (inViewOf(watcher)) {
				seen = true;
				if (beingLookedAt(watcher)) {
					held = true;
					break;
				}
			}
		}
		if (seen) {
			this.witnessed = true;
		}
		this.showBar(watchers);

		// FROM THE FIRST BLOW IN HIS CASTLE, THE DUEL HAS THE TICK. Nothing below
		// this line moves him while it does. See Duel for why there has to be
		// exactly one owner.
		if (this.theDuel.owns(watchers)) {
			this.theDuel.tick(watchers);
			return;
		}

		// A DUEL OUTRANKS EVERYTHING BELOW IT, in either mood. He is not walking
		// past an iron golem to get to somebody.
		if (this.duel()) {
			return;
		}

		// AND SO DOES A BODY, for as long as there is one to walk to.

		// HE IS OVER HIS OWN CASTLE, AND THAT IS WHERE HE LIVES NOW.
		if (this.patrol()) {
			return;
		}

		// THE PROWL, AND IT SITS ABOVE THE STARE ON PURPOSE.
		//
		// Everything below this line is built on "being looked at makes him leave",
		// which is correct for five phases and is the exact inverse of what a prowl
		// wants. Putting it here rather than teaching flee() and closeOn() and the
		// standoff about a new mode keeps all of that untouched: while he is
		// searching, none of those rules have run yet, and the tick that ends the
		// search is the tick they start applying again.
		// HIS OWN BUSINESS IS THE FLOOR, NOT A MODE.
		//
		// This used to read "if prowling, prowl and return" — which made being out
		// here and hunting somebody mutually exclusive, and everything either of
		// them cared about had to be handed over at the boundary. Five separate
		// bugs this session came out of that one line: the opening deleting him,
		// the flying flap, the freeze in mid-air. Every time it was one mode not
		// knowing what the other one had left switched on.
		//
		// He is simply HERE now, permanently, and the rest are things he is doing
		// while he is. Chores when nothing has his attention; the opening or a
		// hunt when something does. Nothing gets handed over because nothing
		// changes hands — the same man carries on, with something else in mind.
		if (this.present) {
			if (this.prowl(watchers)) {
				return;      // nothing has his attention; his own business took it
			}
			// prowl handed off — fall through to whatever it handed off to.
		}

		// AND THE OPENING OWNS EVERY TICK IT RUNS FOR. Above the stare for the same
		// reason the prowl is: while this runs, being looked at must not make him
		// flee — being looked at is the entire routine.

		if (this.fleeing) {
			this.flee(watchers, seen);
			return;
		}


		// You never get to reach him, and it does not matter which of you tries.
		Player closest = null;
		double closestDistance = Double.MAX_VALUE;
		for (Player watcher : watchers) {
			double distance = this.distanceTo(watcher);
			if (distance < closestDistance) {
				closestDistance = distance;
				closest = watcher;
			}
		}


		// He was always standing there and he goes on standing there. Moving
		// while watched would break the only claim the whole event makes.
		// SOMEBODY HAS WALKED UP TO HIM. Out here he is an apparition, and this is the
		// whole of what an apparition does about it: the ones who came close are
		// noted, and he is behind them — or, failing a spot behind them, gone. The
		// fight this used to hand over to lives in his castle now. See Duel.
		if (closest != null && closestDistance < TOO_CLOSE) {
			if (this.level() instanceof ServerLevel paying
				&& paying.getGameTime() >= this.lastDefiance + DEFIANCE_COOLDOWN) {
				this.lastDefiance = paying.getGameTime();
				for (Player watcher : watchers) {
					if (this.distanceTo(watcher) < TOO_CLOSE
						&& watcher instanceof ServerPlayer chaser) {
						Heat.noticed(chaser, DEFIANCE_APPROACHED);
					}
				}
			}
			if (!relocateBehind(watchers)) {
				this.fleeing = true;
			}
			return;
		}

		this.getNavigation().stop();

		if (watchers.isEmpty()) {
			this.vanish("no player within WATCH_RANGE");
			return;
		}

		// AND HE IS FACING YOU. Not "usually", not "after a moment".
		//
		// This was left to LookAtPlayerGoal, which is the wrong tool: that goal
		// picks a target on a probability, holds it for a random number of
		// ticks and then lets go, because it exists to make idle villagers
		// glance at passers-by. Applied here it meant he was often standing at
		// three-quarters profile staring off at a hillside, and a figure that
		// is not looking at you is a figure that has not noticed you — which is
		// the exact opposite of the only thing this event says.
		//
		// Driven straight from the geometry every tick instead, so there is
		// nothing to be probabilistic about. Body and head both, or the head
		// swivels on a body still facing wherever he was put.
		this.faceOneOf(watchers);

		if (seen) {
			this.unseenTicks = 0;
			// EARLY ON, BEING LOOKED AT IS ENOUGH TO END IT.
			//
			// At WATCHER he is gone three and a half seconds after somebody
			// actually has him — enough to find a shape and start walking
			// toward it, nowhere near enough to be sure of anything. That is the correct first encounter: the
			// player has seen something and has nothing to show for it, and
			// every later sighting is measured against a memory they do not
			// trust.
			//
			// The clock is shared and starts on FIRST sight, which is what
			// makes this work with company. Whoever spots him spends it, and
			// anybody still facing the other way arrives at an empty hill.
			//
			// The limit stretches with the phases until it stops existing, so
			// the same act of looking at him gets a longer and longer answer.
			// By HUNTER he simply looks back for as long as you care to stand
			// there, which is only frightening because of how briefly he used
			// to allow it.
			int allowed = this.staredDown();
			// `held`, not `seen`. Being on somebody's screen is not being
			// looked at, and spending the allowance on the first is what left
			// nothing for the second.
			if (allowed > 0 && held && ++this.watchedTicks > allowed) {
				this.vanish("stared down");
			}
		} else if (this.witnessed && ++this.unseenTicks > UNSEEN_GRACE) {
			// Seen, then not seen, then not there. Nobody watches him go; they
			// simply find that he has gone, which is the one version of this
			// they cannot talk themselves out of.
			//
			// It takes EVERY pair of eyes losing him. Two people who split up
			// and keep him between them hold him there far longer than one
			// person can, which is the right reward for co-ordinating — and
			// FLEE_LIMIT still stops it becoming a stalemate.
			this.vanish("seen, then lost by everybody");
		}
	}

	/**
	 * How long he will let himself be looked at.
	 *
	 * Zero means indefinitely. The curve is the whole arc of the mod in one
	 * method: at first he cannot be held in the eye at all, and by the end he
	 * does not mind being seen.
	 */
	private int staredDown() {
		// ZERO MEANS NEVER, and on his own ground he is never stared down. Out here
		// a second and a half — see the note below, which was written about the
		// early chapters and is now simply about being an apparition.
		return APPARITION;
	}

	/** A second and a half. */
	private static final int APPARITION = 70;

	/**
	 * He breaks your line of sight, and then he is not there.
	 *
	 * Moved directly rather than pathfound, and through solid rock rather than
	 * around it. That sounds like cheating and is the opposite: a figure that
	 * has to find a route is a figure the player can corner, and being cornered
	 * would force the honest answer — that he is a mob with a hitbox. Backing
	 * into a cave wall and being gone never has to answer that question.
	 *
	 * He does not sprint away across open ground for long. FLEE_LIMIT is short
	 * because the goal is not escape, it is to be out of sight; the moment the
	 * player's view of him is broken by anything at all, that is the end of it.
	 */
	private void flee(List<Player> from, boolean seen) {
		if (from.isEmpty() || ++this.fleeTicks > FLEE_LIMIT) {
			this.vanish("fled far enough");
			return;
		}

		this.noPhysics = true;
		this.setNoGravity(true);
		this.getNavigation().stop();

		// Away from all of them at once, each pulling in inverse proportion to
		// how close they are. Running from only the nearest would have walked
		// him straight into whoever was flanking, which is exactly the move two
		// players will try the first time they see him.
		Vec3 away = Vec3.ZERO;
		for (Player watcher : from) {
			Vec3 apart = new Vec3(this.getX() - watcher.getX(), 0.0, this.getZ() - watcher.getZ());
			double distance = Math.max(1.0, apart.length());
			away = away.add(apart.normalize().scale(1.0 / distance));
		}
		if (away.lengthSqr() < 1.0E-4) {
			// Surrounded, or dead centre between them. He goes now rather than
			// picking an arbitrary direction and jittering.
			this.vanish("surrounded, nowhere to flee");
			return;
		}
		away = away.normalize();

		// FACING THEM the whole way, and this is the note that matters most.
		//
		// Something that turns its back and runs is frightened, and a
		// frightened thing is one the player has beaten. Backing away while
		// still looking at you is not a retreat at all — it is somebody
		// declining to let you any closer, without once looking away, and it
		// reverses who is in charge of the distance between you.
		Player watching = from.get(0);
		double nearest = Double.MAX_VALUE;
		for (Player candidate : from) {
			double gap = this.distanceToSqr(candidate);
			if (gap < nearest) {
				nearest = gap;
				watching = candidate;
			}
		}
		float yaw = (float)(Math.atan2(watching.getZ() - this.getZ(),
			watching.getX() - this.getX()) * (180.0 / Math.PI)) - 90.0F;
		this.setYRot(yaw);
		this.setYBodyRot(yaw);
		this.setYHeadRot(yaw);

		this.setPos(this.getX() + away.x * FLEE_SPEED,
			this.getY(), this.getZ() + away.z * FLEE_SPEED);

		if (!seen || this.level().getBlockState(this.blockPosition()).isSolid()) {
			this.vanish("broke line of sight while fleeing");
		}
	}

	/**
	 * He is simply not there any more.
	 *
	 * No smoke, no teleport sound. An earlier version had both and they were
	 * the same mistake as glow on his body: a departure effect announces a
	 * supernatural ability, which files him alongside endermen. People do not
	 * dissolve. The rule is that he is never seen arriving — the mirror of it
	 * is that he is never seen leaving, and absence with nothing marking the
	 * transition is far worse than any animation.
	 *
	 * What replaces it is one footstep, once, from somewhere else. It does not
	 * show anything and it does not explain anything. It only says he is not
	 * gone, he has moved — which is the opposite of the closure a puff of
	 * smoke gives you.
	 *
	 * level().playSound rather than this.playSound: he is isSilent(), and that
	 * suppression is wanted everywhere except here.
	 */
	/** Loosely in front of the player, with line of sight. Not aiming at him. */
	private boolean inViewOf(Player player) {
		Vec3 look = player.getViewVector(1.0F).normalize();
		Vec3 toMe = new Vec3(
			this.getX() - player.getX(),
			this.getEyeY() - player.getEyeY(),
			this.getZ() - player.getZ()
		).normalize();
		return look.dot(toMe) > SEEN_CONE && player.hasLineOfSight(this);
	}

	/**
	 * Not "he is on screen" — "you are looking at him".
	 *
	 * These were one test and that is why the timer felt broken. The wide cone
	 * is over a hundred degrees across, so the clock started the instant he
	 * entered the far corner of the player's vision, at fifty blocks, as a
	 * two-pixel smudge. Most of the allowance was spent before anybody had
	 * found him, and what was left was the tail end — which is precisely the
	 * "no time to see it" being reported.
	 *
	 * The wide cone still decides whether he counts as witnessed and whether
	 * everyone has lost him. Only the countdown uses this one, and it means
	 * what it says: he is near the middle of your view and you have him.
	 */
	private boolean beingLookedAt(Player player) {
		Vec3 look = player.getViewVector(1.0F).normalize();
		Vec3 toMe = new Vec3(
			this.getX() - player.getX(),
			this.getEyeY() - player.getEyeY(),
			this.getZ() - player.getZ()
		).normalize();
		return look.dot(toMe) > HELD_CONE && player.hasLineOfSight(this);
	}

	/**
	 * Turn and face whoever has him, or the nearest person if nobody does.
	 */
	private void faceOneOf(List<Player> watchers) {
		Player face = null;
		double nearest = Double.MAX_VALUE;
		for (Player watcher : watchers) {
			if (this.beingLookedAt(watcher)) {
				face = watcher;
				break;
			}
			double distance = this.distanceTo(watcher);
			if (distance < nearest) {
				nearest = distance;
				face = watcher;
			}
		}
		if (face == null) {
			return;
		}
		float yaw = (float)(net.minecraft.util.Mth.atan2(
			face.getZ() - this.getZ(), face.getX() - this.getX()) * (180.0 / Math.PI)) - 90.0F;
		this.setYRot(yaw);
		this.yHeadRot = yaw;
		this.yHeadRotO = yaw;
		this.setYBodyRot(yaw);
		this.getLookControl().setLookAt(face.getX(), face.getEyeY(), face.getZ(), 90.0F, 90.0F);
	}

	/**
	 * Sometimes the lights go with him.
	 *
	 * Approaching and dissolving is one readable pattern, and a pattern you
	 * have read is not frightening. Leaving something behind makes the visit
	 * an event rather than a sighting — and darkness closing in as he goes is
	 * the version that costs the player nothing permanent: every torch is
	 * dropped, and can be put straight back.
	 *
	 * Capped at three, and only ever torches. Anything that strands a player
	 * without a pickaxe or destroys a build is out of bounds (DESIGN.md §9).
	 */
	private void takeTheLight(ServerLevel server, Player player) {
		if (!Config.get().takeTheLight) {
			return;
		}
		BlockPos origin = player.blockPosition();
		int taken = 0;
		int r = 8;
		for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-r, -3, -r), origin.offset(r, 3, r))) {
			if (taken >= 3) {
				break;
			}
			if (!server.getBlockState(pos).is(Blocks.TORCH)
				&& !server.getBlockState(pos).is(Blocks.WALL_TORCH)) {
				continue;
			}
			BlockPos at = pos.immutable();
			server.removeBlock(at, false);
			server.addFreshEntity(new ItemEntity(server,
				at.getX() + 0.5, at.getY() + 0.1, at.getZ() + 0.5,
				new ItemStack(Items.TORCH)));
			server.playSound(null, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5,
				SoundEvents.FIRE_EXTINGUISH, this.getSoundSource(), 0.5F, 1.3F);
			taken++;
		}
	}

	/**
	 * What is left where he was standing.
	 *
	 * From TRESPASSER he leaves a few small fires behind him, which is the
	 * first thing in the mod that says outright that something was there —
	 * every trace before this could be argued with, and a ring of fire on the
	 * grass cannot be.
	 *
	 * FIRE IS THE MOST DANGEROUS BLOCK IN THIS MOD and it gets three separate
	 * safeguards, because burning down somebody's base or their forest by
	 * accident is not a scare, it is the end of their save. It is never placed
	 * where anything nearby can catch; it is never placed on anything that
	 * burns; and every one of them is put out after six seconds whether or not
	 * a player is there to see it.
	 *
	 * Six seconds is long enough to walk back and find it burning, and short
	 * enough that fire spread — which needs random ticks and time — almost
	 * never gets a turn.
	 */
	private void scorch(ServerLevel level) {
		this.scorch(level, 4 + this.random.nextInt(3));
	}

	/**
	 * @param wanted how many to try for. Every one of them still has to pass
	 *               safeToBurn, so this is an intention rather than a promise —
	 *               swinging at him inside a wooden house leaves nothing at all,
	 *               which is exactly right.
	 */
	private void scorch(ServerLevel level, int wanted) {
		if (!Config.get().scorch) {
			return;
		}
		int lit = 0;

		for (int attempt = 0; attempt < 24 && lit < wanted; attempt++) {
			double angle = this.random.nextDouble() * Math.PI * 2.0;
			double range = 1.2 + this.random.nextDouble() * 2.4;
			BlockPos at = BlockPos.containing(
				this.getX() + Math.cos(angle) * range,
				this.getY(),
				this.getZ() + Math.sin(angle) * range);

			BlockPos ground = null;
			for (int down = 0; down <= 3; down++) {
				if (level.getBlockState(at.below(down)).isSolid()) {
					ground = at.below(down);
					break;
				}
			}
			if (ground == null || !level.getBlockState(ground.above()).isAir()) {
				continue;
			}
			if (!safeToBurn(level, ground)) {
				continue;
			}

			BlockPos flame = ground.above();
			level.setBlock(flame, Blocks.FIRE.defaultBlockState(), 2);
			com.bloomlet.herobrine.manifest.Cadence.in(level.getServer(), 120, () -> {
				if (level.getBlockState(flame).is(Blocks.FIRE)) {
					level.setBlock(flame, Blocks.AIR.defaultBlockState(), 2);
				}
			});
			lit++;
		}
	}

	/** Nothing within reach may be able to catch, including the floor itself. */
	private static boolean safeToBurn(ServerLevel level, BlockPos ground) {
		for (BlockPos near : BlockPos.betweenClosed(ground.offset(-2, -1, -2),
				ground.offset(2, 3, 2))) {
			if (level.getBlockState(near).ignitedByLava()) {
				return false;
			}
		}
		return true;
	}


	/**
	 * WHICH of the three is out there, so send() knows what to raise.
	 *
	 * The call sites were passing breakOffs, which counts ROUNDS — the times he has
	 * reached everybody present — and that is a completely different number from
	 * how far through his health the fight is. In a creative-mode playtest it stayed
	 * at nought for five and a half minutes while the damage crossed two thresholds,
	 * so the crowd never grew and would never have changed type either.
	 */


	// ---- END GONE TO GROUND -----------------------------------------------


	/**
	 * ONE OF THREE, AND HE SAYS SOMETHING ABOUT IT.
	 *
	 * The first two put him out of reach again — the same answer a sword has
	 * always got from him — so the player cannot simply stand in one place and
	 * swing three times. They have to connect three separate times, on something
	 * that keeps moving behind them, which is a real thing to have done.
	 *
	 * The third ends the hunt EARLY, and early is the reward. The hundred
	 * seconds he was going to spend are the price of not fighting back, and a
	 * group that drives him off in twenty has bought their windows back — the
	 * ladder only climbs while a pursuit is actually running.
	 *
	 * It still counts as survived, and it still costs them wrath. relent() pays
	 * DEFIANCE_ENDURED to everybody present exactly as it does at the end of a
	 * full-length hunt, because hitting him three times is not a way of avoiding
	 * his attention — it is the loudest way there is of getting it.
	 */
	/**
	 * A GOOD SWORD MOVES HIM, AND A BARE HAND VERY NEARLY DOES NOT.
	 *
	 * He never budged, for a reason that is invisible from the outside: both of the
	 * paths a player's blow takes through hurtServer end in `return false`. He
	 * counts the hit, he takes the damage off his own tally, and he tells vanilla
	 * the damage was refused — which is correct, because his health is not what
	 * this fight is about. But vanilla knockback is applied by the damage code that
	 * was just told nothing happened, so there has never been a shove at all.
	 *
	 * Not zero knockback. NO knockback, from any weapon, ever, and no way to tell
	 * that from a boss with high resistance.
	 *
	 * SO IT IS DONE HERE, FROM THE DAMAGE THAT WAS ACTUALLY ROLLED. Which is the
	 * one number that already knows about the weapon, the enchantments, the
	 * critical and the cooldown — reading the held item instead would be
	 * reimplementing all four and getting the last two wrong.
	 *
	 *     an empty hand   ~1 damage    0.09   a twitch, and it should be
	 *     an iron sword   ~6           0.25
	 *     netherite       ~8           0.31
	 *     netherite, Sharpness V  ~11  0.40   capped
	 *
	 * FOUR TENTHS IS THE CEILING and it is deliberately short of vanilla's own
	 * shove. He is three blocks of him in act three; a boss that skates backwards
	 * off every hit is a boss you can kite into a corner and beat with a stick, and
	 * the whole fight is built on him choosing the distance. What is wanted is the
	 * sword FEELING like a sword, not a crowd-control tool.
	 *
	 * HORIZONTAL ONLY, AND ONLY WITH HIS FEET ON THE GROUND. Lifting him is how
	 * this turns into an iron golem juggling somebody; and shoving him mid-flight
	 * would fight the scripted ascents — the act change, the pause, the way he
	 * hangs over the keep on fire — which own his movement outright while they run.
	 *
	 * The wound window is what makes it visible. See linger: a WOUND does not damp
	 * his motion, so ordinary friction settles him over about half a second, which
	 * is what being hit by something heavy looks like. The same shove during his
	 * OWN window would be eaten in four ticks.
	 */
	/**
	 * HE SOUNDS LIKE A MAN, PLAYED WRONG.
	 *
	 * He had no sound overrides at all, which means PathfinderMob's default: the
	 * GENERIC_HURT grunt every unremarkable mob in the game makes. The one thing he
	 * is supposed to be is a player who is not a player, and he was answering a
	 * sword with the same noise as a bat.
	 *
	 * So it is Steve's own set — the hurt and the death a player makes — DROPPED A
	 * FIFTH. Pitch is the whole trick and it is the right trick: a pitched-down
	 * player sound is recognisably the sound you make, arriving from something that
	 * is not you, and that reads as wrong in a way an invented growl cannot. An
	 * original sound would just be another monster.
	 *
	 * 0.55 is deliberate. Around 0.8 he is a large man; below about 0.4 the sample
	 * turns to mud and stops being identifiable as Steve, which throws away the
	 * only thing being borrowed.
	 *
	 * AND THERE IS SOMETHING UNDER IT. Pitch alone is a big Steve. The soul
	 * escaping, laid beneath the grunt at half speed, is what makes it cursed
	 * rather than merely deep — two sounds at once, one of them human and one of
	 * them not, from a single hit.
	 */
	private static final float CURSED_PITCH = 0.55F;

	@Override
	protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.PLAYER_HURT;
	}

	@Override
	protected net.minecraft.sounds.SoundEvent getDeathSound() {
		return SoundEvents.PLAYER_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return CURSED_PITCH;
	}

	@Override
	protected float getSoundVolume() {
		return 1.1F;
	}

	@Override
	protected void playHurtSound(DamageSource source) {
		super.playHurtSound(source);           // Steve, a fifth down
		if (this.level() instanceof ServerLevel here) {
			here.playSound(null, this.getX(), this.getY(), this.getZ(),
				SoundEvents.SOUL_ESCAPE.value(), this.getSoundSource(), 0.7F, 0.5F);
		}
	}

	private static final double SHOVE_LEAST = 0.09;
	private static final double SHOVE_MOST = 0.40;
	private static final float SHOVE_FULL_AT = 11.0F;

	private void stagger(ServerPlayer striker, float damage) {
		if (!this.onGround() || this.flying) {
			return;
		}
		double from = Math.min(1.0, Math.max(0.0, damage / SHOVE_FULL_AT));
		double push = SHOVE_LEAST + from * (SHOVE_MOST - SHOVE_LEAST);
		net.minecraft.world.phys.Vec3 away = this.position()
			.subtract(striker.position());
		if (away.horizontalDistanceSqr() < 1.0E-4) {
			// Standing inside him. Shove him off the way he is facing instead of
			// dividing by nothing.
			away = net.minecraft.world.phys.Vec3.directionFromRotation(0.0F,
				this.getYRot());
		}
		away = new net.minecraft.world.phys.Vec3(away.x, 0.0, away.z).normalize();
		this.setDeltaMovement(this.getDeltaMovement()
			.add(away.x * push, 0.0, away.z * push));
		this.hurtMarked = true;   // tells the client the shove happened
	}


	/** Which of the two this pause is. Rolled once, when the pause begins. */
	private boolean circling;


	/**
	 * Push it, if it is the kind of thing that pushes.
	 *
	 * Iron is the exception on purpose, and it is the same sentence the cells
	 * downstairs are written in: iron is what holds. canOpenByHand comes off
	 * the block's own BlockSetType rather than a hardcoded list, so it is right
	 * for every wood in the game and for any a mod adds. An iron door still
	 * stops him — he has to cut it, which at hardness five is three seconds of
	 * standing there doing it. That is not an obstacle so much as a receipt for
	 * having built properly.
	 *
	 * @return true if it is open, or has just been opened, and there is nothing
	 *         left here to break
	 */
	/**
	 * Is this a shut thing that opens — and if so, open it.
	 *
	 * openInstead answers "there is nothing to break here", which is true of a door
	 * that was already standing open. Out on a walk that would fire every tick he
	 * spent near one, so this asks the narrower question first: is it CLOSED.
	 *
	 * @return true if something was shut and now is not
	 */
	private boolean swings(ServerLevel here, BlockPos pos) {
		net.minecraft.world.level.block.state.BlockState state = here.getBlockState(pos);
		boolean shut =
			(state.getBlock() instanceof net.minecraft.world.level.block.DoorBlock
				&& !state.getValue(net.minecraft.world.level.block.DoorBlock.OPEN))
			|| (state.is(net.minecraft.tags.BlockTags.WOODEN_TRAPDOORS)
				&& !state.getValue(net.minecraft.world.level.block.TrapDoorBlock.OPEN))
			|| (state.is(net.minecraft.tags.BlockTags.FENCE_GATES)
				&& !state.getValue(net.minecraft.world.level.block.FenceGateBlock.OPEN));
		return shut && this.openInstead(here, pos);
	}

	private boolean openInstead(ServerLevel here, BlockPos pos) {
		net.minecraft.world.level.block.state.BlockState state = here.getBlockState(pos);
		if (state.getBlock() instanceof net.minecraft.world.level.block.DoorBlock door) {
			if (!door.type().canOpenByHand()) {
				return false;   // iron. He cuts it instead.
			}
			if (!state.getValue(net.minecraft.world.level.block.DoorBlock.OPEN)) {
				door.setOpen(this, here, state, pos, true);
			}
			return true;
		}
		// Trapdoors and gates have no accessible type() from outside, so the
		// tags carry it instead. WOODEN_TRAPDOORS excludes the iron one by
		// construction, which is the same iron rule the doors follow.
		if (state.is(net.minecraft.tags.BlockTags.WOODEN_TRAPDOORS)) {
			return this.swingOpen(here, state, pos,
				net.minecraft.world.level.block.TrapDoorBlock.OPEN,
				SoundEvents.WOODEN_TRAPDOOR_OPEN);
		}
		if (state.is(net.minecraft.tags.BlockTags.FENCE_GATES)) {
			return this.swingOpen(here, state, pos,
				net.minecraft.world.level.block.FenceGateBlock.OPEN,
				SoundEvents.FENCE_GATE_OPEN);
		}
		return false;
	}

	private boolean swingOpen(ServerLevel here,
			net.minecraft.world.level.block.state.BlockState state, BlockPos pos,
			net.minecraft.world.level.block.state.properties.BooleanProperty open,
			net.minecraft.sounds.SoundEvent sound) {
		if (!state.getValue(open)) {
			here.setBlock(pos, state.setValue(open, true), 10);
			here.playSound(null, pos, sound, net.minecraft.sounds.SoundSource.BLOCKS,
				1.0F, here.getRandom().nextFloat() * 0.1F + 0.9F);
		}
		return true;
	}

	private void stopBreaking(ServerLevel here) {
		if (this.breaking != null) {
			here.destroyBlockProgress(this.getId(), this.breaking, -1);
			this.breaking = null;
			// Back to the axe, so he is never seen walking about with a shovel.
			this.blade();
		}
		this.breakTicks = 0;
	}

	/**
	 * And it is over.
	 *
	 * The one thing the whole mod has been pointing at, so it must not be a
	 * corpse and a silence. The storm breaks because the wrath that was holding
	 * it goes with him, the clock starts again, and the sun that has not come
	 * up for the whole of SIEGE comes up.
	 *
	 * Wrath goes to zero rather than being nudged down. Everything in this mod
	 * reads off that number — the weather, the clock, the animals, the pacing,
	 * whether he can exist at all — so zeroing it is not a scoring decision, it
	 * is how the world is put back. The player who has just fought for thirty
	 * exchanges gets a morning.
	 *
	 * And it can begin again. Wrath climbs from zero the same way it did the
	 * first time, which is the only ending this particular story can honestly
	 * have: he was never something that could be finished, only something that
	 * could be pushed back.
	 */
	@Override
	public void remove(RemovalReason reason) {
		// Discarded — everybody left his world, or Whereabouts sent a duplicate back.
		// The bar is a static-lifetime object on the clients until somebody says so.
		if (this.bar != null) {
			this.bar.removeAllPlayers();
		}
		super.remove(reason);
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (!(this.level() instanceof ServerLevel here)) {
			return;
		}
		here.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
			this.getX(), this.getY() + 1.0, this.getZ(), 140, 0.8, 1.2, 0.8, 0.12);
		here.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
			this.getX(), this.getY() + 1.0, this.getZ(), 90, 1.2, 1.4, 1.2, 0.03);
		// The biggest moment in the mod, and the only time it happens. It should
		// be audible from wherever anybody on the server happens to be standing,
		// and it should come back off the hills afterwards.
		com.bloomlet.herobrine.sound.ModSounds.roll(here, this.blockPosition(),
			com.bloomlet.herobrine.sound.ModSounds.GONE, 5.0F, 1.0F);

		net.minecraft.server.MinecraftServer server = here.getServer();
		// Clear, and stop raining. Skies will not undo a storm it did not start
		// and Nights will not restart a clock it did not stop, so the phase
		// dropping is what releases both — but the current storm has five
		// minutes left on it and would sit there over the ending.
		server.setWeatherParameters(6000, 0, false, false);

		// REMOVED HEROBRINE.
		//
		// The switch every haunting tick handler checks. Without it the rain stopped
		// for five minutes and then the world went back to being his: Nights held
		// the clock at midnight because the story was still SIEGE, Skies rolled the
		// next storm, TheTurning put another wrong villager in the town, and
		// Whereabouts stood a fresh one of him over the keep he had just died in.
		com.bloomlet.herobrine.wrath.Wrath.remove(server);
		here.setAttached(com.bloomlet.herobrine.wrath.Wrath.CLEAR_SKY, true);

		// AND IT IS MORNING. Not "the night ends when it ends" — he held it at
		// midnight for as long as he lived, and the first thing that should happen
		// when he is gone is the sun. The overworld clock is moved to DAY, which is
		// dawn; his own world has no clock to move, its dark is a timeline baked
		// into the dimension, so over there it is the rain stopping that says it.
		java.util.Optional<? extends net.minecraft.core.Holder<net.minecraft.world.clock.WorldClock>>
			clock = server.overworld().registryAccess()
				.get(net.minecraft.world.clock.WorldClocks.OVERWORLD);
		clock.ifPresent(held -> server.clockManager().moveToTimeMarker(held,
			net.minecraft.world.clock.ClockTimeMarkers.DAY));

		// AND THE PEOPLE HE TURNED GET BETTER. Every Turned in a loaded chunk, in any
		// dimension, is a villager again before the smoke clears; the ones in
		// unloaded chunks cure themselves on load — see TurnedEntity.redeem.
		int cured = 0;
		for (ServerLevel each : server.getAllLevels()) {
			for (TurnedEntity turned : each.getEntities(ModEntities.TURNED, e -> e.isAlive())) {
				turned.redeem();
				cured++;
			}
		}
		HerobrineMod.LOGGER.info("morning: the clock moved to day, {} of the turned cured", cured);

		// THE BAR SAYS IT, THEN GOES. It used to stay on screen at whatever it last
		// read, because nothing ever told the clients he was gone. Three seconds of
		// the patch note, empty, then removed for everyone who had it.
		this.bar.setName(net.minecraft.network.chat.Component.literal("Removed Herobrine."));
		this.bar.setProgress(0.0F);
		final net.minecraft.server.level.ServerBossEvent said = this.bar;
		com.bloomlet.herobrine.manifest.Cadence.in(server, 60, said::removeAllPlayers);

		// AND THE SCREEN SAYS IT, AND THE SONG PLAYS. The title is the joke, the
		// running gag and the honest ending all at once; the music is the client's
		// own "Minecraft", the track everybody knows, over the moment the world
		// stops being his. Whatever was playing stops first so they do not overlap.
		// The rain packets go with it: HisWeather forces the level's rain every tick,
		// vanilla only tells clients about a level it changed itself, so a client
		// standing in his world would have gone on drawing rain in a dry sky.
		for (ServerPlayer witness : here.players()) {
			witness.connection.send(new net.minecraft.network.protocol.game
				.ClientboundSetTitlesAnimationPacket(20, 100, 40));
			witness.connection.send(new net.minecraft.network.protocol.game
				.ClientboundSetSubtitleTextPacket(
					net.minecraft.network.chat.Component.literal("§7The rain stops.")));
			witness.connection.send(new net.minecraft.network.protocol.game
				.ClientboundSetTitleTextPacket(
					net.minecraft.network.chat.Component.literal("§fRemoved Herobrine.")));
			witness.connection.send(new net.minecraft.network.protocol.game
				.ClientboundStopSoundPacket(null, net.minecraft.sounds.SoundSource.MUSIC));
			here.playSound(null, witness, com.bloomlet.herobrine.sound.ModSounds.THE_ENDING,
				net.minecraft.sounds.SoundSource.MUSIC, 1.0F, 1.0F);
			com.bloomlet.herobrine.manifest.HisWeather.tellDry(witness);
		}

		// AND HE IS WORTH SOMETHING. The dragon gives twelve thousand experience; this
		// gives four, in a burst, and what he was holding — the sword itself, with
		// everything that was on it, which is the only trophy that means anything —
		// plus the apples he kept in his own chests, diamonds, and a totem, because
		// "removed" is a word that cuts both ways.
		// AT THE KILLER'S FEET. aftermath() opens the way home ON HIS CORPSE, and
		// items and orbs travel through portals: the first kill's whole reward went
		// through to the overworld before the player had turned round. Whoever landed
		// the last blow gets it where they stand; if nobody is left, where he fell.
		ServerPlayer taker = source.getEntity() instanceof ServerPlayer who
			? who : here.getNearestPlayer(this, 64.0) instanceof ServerPlayer near ? near : null;
		Vec3 at = taker != null ? taker.position() : this.position();
		net.minecraft.world.entity.ExperienceOrb.award(here, at, 4000);
		java.util.List<ItemStack> spoils = new java.util.ArrayList<>();
		if (!this.getMainHandItem().isEmpty()) {
			spoils.add(this.getMainHandItem().copy());
		}
		spoils.add(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 3));
		spoils.add(new ItemStack(Items.DIAMOND, 12 + this.random.nextInt(9)));
		spoils.add(new ItemStack(Items.NETHERITE_INGOT, 4));
		spoils.add(new ItemStack(Items.TOTEM_OF_UNDYING));
		for (ItemStack stack : spoils) {
			net.minecraft.world.entity.item.ItemEntity drop =
				new net.minecraft.world.entity.item.ItemEntity(here,
					at.x, at.y + 1.0, at.z, stack);
			drop.setDeltaMovement((this.random.nextDouble() - 0.5) * 0.3, 0.25,
				(this.random.nextDouble() - 0.5) * 0.3);
			drop.setUnlimitedLifetime();
			here.addFreshEntity(drop);
		}
		// The total that used to be zeroed here does not exist any more. Nothing
		// replaces it: the story stays where it is because the story is what the
		// player earned, and heat expires on its own within the minute.

		// And something to walk back to in the morning.
		ServerPlayer killer = source.getEntity() instanceof ServerPlayer who
			? who : here.getNearestPlayer(this, 64.0) instanceof ServerPlayer near
				? near : null;
		if (killer != null) {
			com.bloomlet.herobrine.manifest.Reckoning.aftermath(here,
				this.blockPosition(), killer);
		}

		for (ServerPlayer survivor : here.players()) {
			survivor.sendSystemMessage(net.minecraft.network.chat.Component.literal(
				"§7The rain stops."));
			survivor.sendSystemMessage(net.minecraft.network.chat.Component.literal(
				"§7Somewhere behind you, it is getting light."));
		}
		HerobrineMod.LOGGER.info("he is dead. wrath reset, weather cleared");
	}

	/**
	 * What he is carrying, and it is never nothing.
	 *
	 * Empty hands make him look like he is out for a walk. The axe is the
	 * default because it reads as a weapon at a distance and as a tool up
	 * close, which is exactly what he uses it for.
	 *
	 * COSMETIC, DELIBERATELY. An item in a mob's main hand applies its own
	 * attribute modifiers, so handing him a diamond axe would silently take him
	 * from four damage to thirteen and every number tuned above would be a
	 * lie — and it would move again the moment the tool swapped to a pickaxe.
	 * Clearing ATTRIBUTE_MODIFIERS makes the thing purely something he is
	 * holding, so what he hits for is what STRIKE_DAMAGE says and nothing else.
	 */
	/**
	 * WHAT HE CARRIES WHEN HE IS NOT WORKING.
	 *
	 * An axe was the right tool and the wrong weapon. It is what he breaks into
	 * houses with, and it made the fight read as a man with a job — a lumberjack
	 * who had come for you. A sword is not a tool. Nobody carries one to do
	 * anything else, so the moment it is in his hand the only question left is who
	 * it is for.
	 *
	 * AND IT IS THE WORST ONE IN THE GAME. Sharpness ten, Fire Aspect five,
	 * Knockback five — all far past the anvil ceiling, because nothing about him
	 * was ever obtainable and the tooltip should say so out loud. Knockback five
	 * is the one you feel: he does not kill you with it, he throws you across the
	 * clearing and then walks after you.
	 *
	 * Unbreaking and Mending on top, which do nothing at all mechanically. They
	 * are there because a player who gets a look at it should be able to tell it
	 * was never going to wear out.
	 */
	/**
	 * THE ARM, EVERY TIME, WITHOUT ASKING.
	 *
	 * LivingEntity.swing is a REQUEST, not an instruction. It refuses outright if a
	 * swing is already half-run — `!swinging || swingTime >= duration / 2` — and
	 * everything about how this entity moves makes that condition unreliable:
	 * snapTo and blink reset the client's copy of him mid-animation, the duel and
	 * the lunge and the ordinary melee all swing on separate clocks that can land
	 * on the same tick, and a declined swing is silent. The blow lands, the arm
	 * does not move, and there is nothing in any log to say so.
	 *
	 * Both fields are public. Cleared first, so the animation is a CONSEQUENCE of
	 * the hit rather than a request that might be turned down — and sent with
	 * updateSelf, so it goes out to everybody watching rather than only to the
	 * chunk's trackers.
	 *
	 * Honest note: I could not reproduce the exact declined swing from the source.
	 * This makes it unconditional instead of chasing which of the three clocks was
	 * eating it.
	 */
	private boolean armed;

	private void swipe() {
		this.swinging = false;
		this.swingTime = -1;
		this.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
		// AND THE ONE THAT ACTUALLY MOVES THE ARM. See SWUNG.
		if (this.level() instanceof ServerLevel here) {
			this.setAttached(SWUNG, here.getGameTime());
		}
	}

	private void blade() {
		ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
		sword.set(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS,
			net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY);
		if (this.level() instanceof ServerLevel here) {
			var book = here.registryAccess().lookup(
				net.minecraft.core.registries.Registries.ENCHANTMENT);
			if (book.isPresent()) {
				bite(sword, book.get(),
					net.minecraft.world.item.enchantment.Enchantments.SHARPNESS, 10);
				bite(sword, book.get(),
					net.minecraft.world.item.enchantment.Enchantments.FIRE_ASPECT, 5);
				bite(sword, book.get(),
					net.minecraft.world.item.enchantment.Enchantments.KNOCKBACK, 5);
				bite(sword, book.get(),
					net.minecraft.world.item.enchantment.Enchantments.SWEEPING_EDGE, 5);
				bite(sword, book.get(),
					net.minecraft.world.item.enchantment.Enchantments.UNBREAKING, 10);
				bite(sword, book.get(),
					net.minecraft.world.item.enchantment.Enchantments.MENDING, 1);
			}
		}
		this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, sword);
	}

	private void bite(ItemStack on,
			net.minecraft.core.HolderLookup.RegistryLookup<
				net.minecraft.world.item.enchantment.Enchantment> book,
			net.minecraft.resources.ResourceKey<
				net.minecraft.world.item.enchantment.Enchantment> which, int level) {
		book.get(which).ifPresent(held ->
			on.enchant(held, level));
	}


	/**
	 * How tall the thing directly in his way is, in blocks.
	 *
	 * Looks at the column one step along the line to the player and counts the
	 * solid blocks stacked from his own feet upward. Zero means the way is
	 * clear and he should simply walk.
	 */
	private int wallAhead(Player quarry) {
		return this.wallAhead(quarry.getX(), quarry.getZ());
	}

	private int wallAhead(double tx, double tz) {
		Vec3 flat = new Vec3(tx - this.getX(), 0.0, tz - this.getZ());
		if (flat.lengthSqr() < 1.0E-4) {
			return 0;
		}
		Vec3 step = flat.normalize();
		BlockPos ahead = BlockPos.containing(
			this.getX() + step.x, this.getY(), this.getZ() + step.z);
		int height = 0;
		while (height < SCAN && this.level().getBlockState(ahead.above(height)).blocksMotion()) {
			height++;
		}
		// A block he can simply step onto is not an obstacle at all — the raised
		// STEP_HEIGHT swallows it, and treating it as one would have him
		// hopping over every kerb.
		return height <= 1 ? 0 : height;
	}

	/**
	 * Over it, in one movement.
	 *
	 * The impulse is worked out from the height rather than fixed, because a
	 * jump tuned for a fence looks feeble at a four-block cliff and one tuned
	 * for the cliff sends him sailing over a fence. Vanilla's 0.42 clears about
	 * a block and a quarter and height goes as the square of the launch speed,
	 * so the rest is arithmetic — plus a tenth for the margin, since falling
	 * just short of the ledge is the one outcome that looks broken.
	 */
	/**
	 * Over the hole, the way a sprinting player goes over it.
	 *
	 * Looks along the line to the quarry: if the next step is into nothing, it
	 * walks outward for somewhere to land at roughly this height and jumps for it.
	 * Two to four blocks, which is a player's range — three is comfortable, four is
	 * the one you have to mean, and five is not available to anybody.
	 *
	 * TREE TO TREE FALLS OUT OF THIS FOR FREE. Leaves count as a floor since
	 * ConfinedPlacement was fixed, so a branch four blocks away with air between is
	 * exactly the same problem as a ravine and gets exactly the same answer.
	 *
	 * @return true if he jumped, and the tick is spent
	 */
	/**
	 * AND ALL THREE OF THESE ONLY EVER WANTED A DIRECTION.
	 *
	 * leap, wallAhead and vault each opened with the same line — the flat vector
	 * from him to the quarry — and then never looked at the player again. Taking a
	 * Player was an accident of where they were written, and it is the only reason
	 * the whole repertoire was locked inside the hunt.
	 *
	 * A position instead, and the prowl gets the same legs the chase has: he jumps
	 * gaps, he vaults ledges, and he stops being a man who can be defeated by a
	 * stream. Two things deliberately do NOT come with them — flight, because a
	 * walking man is the entire premise of a prowl, and breaking through, because
	 * he only ever breaks a block because he wanted the block. Their replacement
	 * out here is slipTo: he steps through, and only where nobody can see.
	 */
	private boolean leap(Player quarry) {
		return this.leap(quarry.getX(), quarry.getZ());
	}

	private boolean leap(double tx, double tz) {
		Vec3 flat = new Vec3(tx - this.getX(), 0.0, tz - this.getZ());
		if (flat.lengthSqr() < 1.0E-4) {
			return false;
		}
		flat = flat.normalize();
		BlockPos feet = this.blockPosition();

		// Is the next step actually into nothing? One block out, and nothing to
		// stand on. If there is a floor there this is an ordinary walk and the
		// navigation can have it.
		BlockPos step = BlockPos.containing(
			this.getX() + flat.x, this.getY(), this.getZ() + flat.z);
		if (this.level().getBlockState(step.below()).blocksMotion()) {
			return false;
		}

		for (int out = 2; out <= LEAP_REACH; out++) {
			BlockPos far = BlockPos.containing(
				this.getX() + flat.x * out, this.getY(), this.getZ() + flat.z * out);
			for (int dy = 1; dy >= -2; dy--) {
				BlockPos landing = far.above(dy);
				if (!this.level().getBlockState(landing.below()).blocksMotion()
					|| this.level().getBlockState(landing).blocksMotion()
					|| this.level().getBlockState(landing.above()).blocksMotion()) {
					continue;
				}
				// Enough forward to clear it and enough lift to arrive on top
				// rather than into the side of it.
				double push = 0.19 * out;
				this.setDeltaMovement(flat.x * push, LEAP_LIFT + Math.max(0, dy) * 0.12,
					flat.z * push);
				this.hurtMarked = true;
				this.getNavigation().stop();
				return true;
			}
		}
		return false;
	}

	/** A player's running jump, and not a block more. */
	private static final int LEAP_REACH = 4;
	private static final double LEAP_LIFT = 0.44;

	private void vault(Player quarry, int height) {
		this.vault(quarry.getX(), quarry.getZ(), height);
	}

	private void vault(double tx, double tz, int height) {
		Vec3 flat = new Vec3(tx - this.getX(), 0.0, tz - this.getZ()).normalize();
		double lift = 0.42 * Math.sqrt(height / 1.25) * 1.1;
		this.setDeltaMovement(flat.x * 0.34, lift, flat.z * 0.34);
		// 26.2 has no hasImpulse; hurtMarked is what forces the velocity down
		// to the client now. Without it the server knows he jumped and the
		// player watches him slide up the wall.
		this.hurtMarked = true;
	}

	private int saidFlying = -1000;

	private void takeOff() {
		if (this.flying) {
			return;
		}
		// NOT ONCE HE IS BOUND — unless this is the pause, which sets ascending
		// before it calls here. Every other reason to go up is refused at the door,
		// which is how twenty-one call sites were made safe with one line.
		if (this.bound && this.ascending <= 0) {
			return;
		}
		this.flying = true;
		this.flyTicks = 0;
		this.setNoGravity(true);
		// And once every ten seconds at most. A line printed sixteen times in four
		// seconds tells nobody anything, and it buried every swing in the log.
		if (this.age > this.saidFlying + 200) {
			this.saidFlying = this.age;
			HerobrineMod.LOGGER.info("hunt: going over");
		}
	}


	/**
	 * HE IS NOT THERE, AND THEN HE IS BESIDE YOU.
	 *
	 * The safety net under every piece of geometry nobody thought of: a tree, a
	 * one-block pillar, a boat, a ledge on a cliff, a hole in a roof. The flight
	 * gets six seconds to solve it honestly — the player should get to WATCH him
	 * come, because a figure climbing toward you is worth far more than one that
	 * appears — and if six seconds was not enough, the geometry stops being
	 * allowed to matter.
	 *
	 * Which is not a cheat so much as the rest of the mod finally applying here.
	 * He goes through rock. He relocates behind you when you charge him. He does
	 * not obey doors. The one thing he was still obeying was gravity, and a
	 * player who worked out that a tree beats him had found the only place in
	 * forty hours where the answer was "he gives up".
	 *
	 * Standing room only — a block they could stand in themselves, at their own
	 * level, close but not inside them. If there is nowhere, he lands and tries
	 * the honest way again.
	 *
	 * @return true if he got there
	 */
	private boolean alongside(Player quarry) {
		if (!(this.level() instanceof ServerLevel here)) {
			return false;
		}
		BlockPos them = quarry.blockPosition();
		for (int attempt = 0; attempt < 24; attempt++) {
			double angle = this.random.nextDouble() * Math.PI * 2.0;
			double range = 1.6 + this.random.nextDouble() * 2.4;
			BlockPos at = them.offset(
				(int)Math.round(Math.cos(angle) * range),
				this.random.nextInt(3) - 1,
				(int)Math.round(Math.sin(angle) * range));
			if (!ConfinedPlacement.canStand(here, at) || at.closerThan(them, 1.2)) {
				continue;
			}
			this.flying = false;
			this.setNoGravity(false);
			this.flyTicks = 0;
			this.stuckTicks = 0;
			this.lastDistance = Double.MAX_VALUE;
			float yaw = (float)(net.minecraft.util.Mth.atan2(
				quarry.getZ() - at.getZ(), quarry.getX() - at.getX())
				* (180.0 / Math.PI)) - 90.0F;
			this.blink(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, yaw);
			HerobrineMod.LOGGER.info("hunt: could not reach {} the honest way — "
				+ "went to them instead", quarry.getName().getString());
			return true;
		}
		return false;
	}

	/**
	 * Are they genuinely off the ground — flying — rather than merely up something?
	 *
	 * Four blocks of clear air under their feet. A player on a roof, up a tree or on
	 * a pillar has a floor and can be arrived at; a player on an elytra or in
	 * creative has nothing under them for a long way, and that is a different
	 * problem which only flight answers.
	 */
	private boolean aloft(Player quarry) {
		if (quarry.onGround()) {
			return false;
		}
		BlockPos under = quarry.blockPosition();
		for (int down = 1; down <= 4; down++) {
			if (this.level().getBlockState(under.below(down)).blocksMotion()) {
				return false;
			}
		}
		return true;
	}

	private void land() {
		this.flying = false;
		this.setNoGravity(false);
		this.fallDistance = 0.0;
		this.lastDistance = Double.MAX_VALUE;
		this.stuckTicks = 0;
	}

	/**
	 * Give up on the route and simply be closer, out of sight.
	 *
	 * Behind them and unseen, never in front and never where they are looking,
	 * so it still obeys the oldest rule in the mod: he is not seen arriving.
	 * The player loses him behind a hill, and the next time they check over
	 * their shoulder the gap has halved.
	 */
	/**
	 * Whatever was standing there is not standing there now.
	 *
	 * Killed properly rather than discarded, so it drops what it would have
	 * dropped and a player who finds the bodies can pick up the beef. A pen
	 * quietly emptied with no items in it reads as a despawn bug; a pen full of
	 * dead cows and their drops reads as something having come through.
	 *
	 * Animals only — no villagers, and nothing anybody tamed. A dead dog is a
	 * different and much crueller event than this is reaching for, and it is one
	 * the mod already has a place for elsewhere.
	 */
	/**
	 * AND THE ONES THAT CANNOT HURT HIM DO NOT GET AWAY WITH IT EITHER.
	 *
	 * The hurt path only fires when something actually lands damage, and half the
	 * things that pick a fight with him cannot. A snow golem throws snowballs that
	 * do nothing at all; a wolf on a player who is standing behind him will bite
	 * and be shrugged off. Under the hurt rule alone those would circle him
	 * indefinitely, unharmed and unanswered, which is the same weak picture the
	 * pillagers made.
	 *
	 * So anything that has CHOSEN him, close enough to be doing something about it,
	 * stops — whether or not it managed to land a blow first.
	 */
	private static final double ANSWERS_AT = 12.0;

	private int saidStopped = -1000;

	private void answer(ServerLevel field) {
		for (net.minecraft.world.entity.Mob mob : field.getEntitiesOfClass(
				net.minecraft.world.entity.Mob.class,
				this.getBoundingBox().inflate(ANSWERS_AT),
				m -> m.isAlive() && m.getTarget() == this
					&& !com.bloomlet.herobrine.manifest.TheHunt.isHis(m))) {
			if (challenger(mob)) {
				// It gets a duel, not a death. One place does the fighting so the
				// facing, the pacing and the interruption cannot disagree.
				if (this.busyWith == null || !this.busyWith.isAlive()) {
					this.nowDealWith(mob);
				}
				continue;
			}
			field.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
				mob.getX(), mob.getY() + 0.6, mob.getZ(), 16, 0.3, 0.5, 0.3, 0.02);
			mob.hurtServer(field, this.damageSources().mobAttack(this), Float.MAX_VALUE);
			// One line a second at most. An evoker pouring out vexes put a hundred
			// and fifty of these in three minutes and buried everything else.
			if (this.age > this.saidStopped + 20) {
				this.saidStopped = this.age;
				HerobrineMod.LOGGER.info("{} chose him and stopped",
					mob.getType().toShortString());
			}
		}
	}


	/**
	 * HE COMES TO YOU — up the pillar, or down into the cave.
	 *
	 * The two places a pursuer on foot simply loses, and neither of them is a
	 * base. A tower of dirt is not a build and a hole is not a build; they are
	 * both the same move, which is to stand somewhere his feet cannot follow and
	 * wait for the event to expire. Flying was the old answer and it is a worse
	 * one twice over — the glide has been a bug farm for its whole life, and
	 * watching him circle below you for twenty seconds is not frightening.
	 *
	 * NOT reappearAt, because reappearAt asks the heightmap for the top of the
	 * column and would put him in the field above your cave every single time.
	 * ConfinedPlacement.nearby floods through the air you are actually standing
	 * in, so it can only ever return somewhere connected to you.
	 *
	 * Two to seven blocks, and it does not care whether you are looking. He is
	 * not sneaking up; he is arriving, and he should be inside reach the moment
	 * he does — otherwise this just moves the stalemate closer.
	 *
	 * SIGHT IS REQUIRED, AND THE OLD COMMENT HERE ARGUED THE OPPOSITE.
	 *
	 * It said: round the corner of the same cave is a perfectly good place to turn
	 * up, and demanding a clear line would refuse every twisting tunnel in the game
	 * — which is most of them, and exactly where somebody hiding from him will be.
	 *
	 * Every word of that is true and it is the argument FOR the change. "Exactly
	 * where somebody hiding from him will be" is a description of hiding working.
	 * The reason this existed is that he lost to a hole in the ground; he is allowed
	 * to lose to a hole in the ground, and out here that is the point — the answer
	 * to him is not supposed to be in the overworld.
	 *
	 * So he can still cross a cave in an instant. He has to have seen you in it.
	 */
	private boolean closeIn(Player quarry) {
		if (!(this.level() instanceof ServerLevel here)
			|| !(quarry instanceof ServerPlayer theirs)
			|| !this.hasLineOfSight(quarry)) {
			return false;
		}
		// Two blocks under them at the most. Up a tree that means the branch or
		// nothing, and nothing is the right answer — it falls through to flight,
		// which can actually get there.
		BlockPos at = ConfinedPlacement.nearby(here, theirs, 2.0, 7.0, false, false, 2);
		if (at == null) {
			return false;
		}
		// AND NOT INSIDE SOMEBODY'S HOUSE.
		//
		// The shelter test on goneToGround covers the player who seals up and
		// stays still; it does nothing for the player who seals up and keeps
		// moving, and that one would have had him materialise in the kitchen.
		// A wall is answered by taking the wall apart, which he already does and
		// which the player can hear coming — the whole reason a build is worth
		// putting up is that getting through it is work. Stepping through it is
		// not a scare, it is the end of anybody bothering to build.
		if (com.bloomlet.herobrine.manifest.Hearth.built(here, at)
			>= com.bloomlet.herobrine.manifest.Hearth.ENOUGH) {
			return false;
		}
		if (this.flying) {
			this.land();
		}
		this.stopBreaking(here);
		this.blink(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, this.getYRot());
		this.lastDistance = Double.MAX_VALUE;
		HerobrineMod.LOGGER.info("hunt: beaten on foot — he arrives {} blocks from {}",
			(int) Math.sqrt(at.distSqr(theirs.blockPosition())),
			theirs.getName().getString());
		return true;
	}

	/**
	 * Be somewhere else, chosen rather than stumbled into.
	 *
	 * @param wantSeen true when the point is to be LOOKED at — the standing-off
	 *                 half of a hunt only works if the player actually finds
	 *                 him out there; false when he is coming back in, because
	 *                 the oldest rule in the mod is that he is never seen
	 *                 arriving.
	 */
	private boolean reappearAt(Player quarry, double min, double max, boolean wantSeen) {
		if (!(this.level() instanceof ServerLevel here)) {
			return false;
		}
		// Whatever he was doing, he is on the ground where he turns up, and not
		// still credited with a block he has walked away from.
		if (this.flying) {
			this.land();
		}
		this.stopBreaking(here);
		for (int attempt = 0; attempt < 40; attempt++) {
			double angle = this.random.nextDouble() * Math.PI * 2.0;
			double range = min + this.random.nextDouble() * (max - min);
			int x = net.minecraft.util.Mth.floor(quarry.getX() + Math.cos(angle) * range);
			int z = net.minecraft.util.Mth.floor(quarry.getZ() + Math.sin(angle) * range);
			int y = here.getHeight(
				net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			BlockPos at = null;
			for (int down = 0; down <= 4 && at == null; down++) {
				BlockPos maybe = new BlockPos(x, y - down, z);
				if (ConfinedPlacement.canStand(here, maybe)) {
					at = maybe;
				}
			}
			if (at == null) {
				continue;
			}

			Vec3 look = quarry.getViewVector(1.0F).normalize();
			Vec3 toSpot = new Vec3(at.getX() + 0.5 - quarry.getX(),
				at.getY() - quarry.getEyeY(), at.getZ() + 0.5 - quarry.getZ()).normalize();
			boolean inFront = look.dot(toSpot) > (wantSeen ? 0.35 : 0.1);

			if (wantSeen) {
				// In front of them AND actually visible from where they stand.
				// A spot behind a hill satisfies the cone and wastes the whole
				// pause — they turn, see nothing, and decide it is over.
				if (!inFront || !clearTo(here, quarry, at)) {
					continue;
				}
			} else if (inFront) {
				continue;
			}

			this.blink(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, this.getYRot());
			this.lastDistance = Double.MAX_VALUE;
			return true;
		}
		return false;
	}

	/**
	 * HE IS NOT SEEN CROSSING THE FIELD, AND HE WAS.
	 *
	 * Every relocation in this class called snapTo, which is a server-side
	 * teleport and reads on the client as nothing of the kind. The position
	 * arrives as a sync packet, and InterpolationHandler smears it over three
	 * ticks — so a forty-block jump is drawn as forty blocks of travel in a
	 * seventh of a second. The player sees a figure DRAG across their view and
	 * stop, which tells them exactly where he went, that he moved rather than
	 * reappeared, and that this is an entity being teleported by a mod.
	 *
	 * That is the single oldest rule here broken by a rendering default: he is
	 * never seen arriving, and he had been visibly arriving all along.
	 *
	 * THE FIX IS NOT TO TURN INTERPOLATION OFF. Setting the handler's length to
	 * zero does stop the smear — interpolateTo snaps outright at zero — but it
	 * is a property of the entity rather than of the move, so it would also snap
	 * his WALKING. Position updates reach the client about three times a second;
	 * interpolation is what turns those into a walk, and without it the figure
	 * that comes across the field at you stutters.
	 *
	 * So the streak is hidden instead of prevented. He goes invisible, moves,
	 * and comes back a fifth of a second later — which is not a workaround, it
	 * is the behaviour this mod has always claimed: he is not there, and then he
	 * is somewhere else. Whatever the renderer does in between now happens to
	 * something nobody can see.
	 */
	private static final int BLINK_TICKS = 4;

	private void blink(double x, double y, double z, float yaw) {
		this.setInvisible(true);
		// SEEN LEAVING, SEEN ARRIVING — in the duel. The hunt's blink is silent on
		// purpose, because arriving behind somebody in the dark is the point of it.
		// In the castle he is a fighter repositioning, and a reposition you cannot
		// read is a cheat, so both ends get smoke and the sound.
		if (this.bound && this.level() instanceof ServerLevel from) {
			this.announceBlink(from, this.getX(), this.getY(), this.getZ());
		}
		this.snapTo(x, y, z, yaw, 0.0F);
		if (this.bound && this.level() instanceof ServerLevel to) {
			this.announceBlink(to, x, y, z);
		}
		if (this.level() instanceof ServerLevel here) {
			com.bloomlet.herobrine.manifest.Cadence.in(here.getServer(), BLINK_TICKS, () -> {
				// He may have discarded himself in the meantime — the hunt can
				// end on any of the four ticks this waits.
				if (this.isAlive()) {
					this.setInvisible(false);
				}
			});
		} else {
			this.setInvisible(false);
		}
	}

	/** Nothing solid between their eye and where his head would be. */
	private static boolean clearTo(ServerLevel level, Player quarry, BlockPos at) {
		Vec3 head = new Vec3(at.getX() + 0.5, at.getY() + 1.7, at.getZ() + 0.5);
		return level.clip(new net.minecraft.world.level.ClipContext(
			quarry.getEyePosition(), head,
			net.minecraft.world.level.ClipContext.Block.COLLIDER,
			net.minecraft.world.level.ClipContext.Fluid.NONE, quarry))
			.getType() == net.minecraft.world.phys.HitResult.Type.MISS;
	}


	/**
	 * He reaches you.
	 *
	 * He is still invulnerable and still relocates the moment anybody swings
	 * back, so this does not make him a mob to be killed — the ending has to
	 * keep that. What it makes him is something with a cost attached, so
	 * standing your ground stops being free and running becomes a decision
	 * rather than a preference.
	 */
	private void strike(ServerPlayer player) {
		if (!(this.level() instanceof ServerLevel here)) {
			return;
		}
		// NOT THROUGH A WALL.
		//
		// doHurtTarget does not test this and neither did anything here, so
		// standing on the far side of two blocks of stone was close enough to
		// be hit — which is the one thing guaranteed to read as broken rather
		// than as frightening. Vanilla melee goals check the same thing before
		// swinging; the difference is that they own the check and this had to
		// be given one.
		if (!this.hasLineOfSight(player)) {
			return;
		}
		// HE HAS STOPPED. HE DOES NOT GET ONE MORE.
		//
		// This is what killed the second hunt in the playtest, one second after he
		// had decided not to: "Robin is at 2,3 — he stops instead of finishing it",
		// then "done after 975 ticks", then "swung at 2,7 blocks, landed=true", then
		// "nobody left to follow".
		//
		// The mercy below is a condition on the SWING, so once brokenOff was set the
		// condition stopped being true — and execution fell straight through into
		// the attack it was supposed to prevent. Guarding the mercy without guarding
		// the blow meant the guard actively turned the mercy off.
		//
		// Third time this shape has bitten: the chest tower, the taunt storm, and
		// now this. Every one of them was a sibling code path that did not know he
		// had already finished. So the check goes at the top, before anything can
		// read a stale condition and act on it.
		if (this.brokenOff || this.relenting) {
			return;
		}

		long now = here.getGameTime();
		if (now < this.lastStruck + STRIKE_COOLDOWN) {
			return;
		}
		this.lastStruck = now;

		// HE FINISHES THEM. THE MERCY IS GONE.
		//
		// There used to be a block here that refused the killing blow: on the tick
		// a swing would have been lethal, the arm did not come down, the fire came
		// off them and he withdrew instead. The reasoning was that a hunt sites the
		// church rather than empties an inventory, and that a death was expensive,
		// unwitnessed and free.
		//
		// It was wrong in play for a reason no amount of precision on the trigger
		// could fix: HEALTH DOES NOT COME BACK ON ITS OWN INSIDE A FIGHT. Brought
		// to four, you are at four for the rest of it, so a threshold meant to fire
		// once fires on every approach forever. The playtest log carries
		// "Robin is at 4,0 — he stops the blow and gives ground" four times across
		// ninety seconds, and the whole last third of that hunt was a man walking up
		// to somebody and declining. A player who notices can park at two hearts and
		// be permanently untouchable by the strongest thing in the mod.
		//
		// So he kills you, the same way he kills a golem, a pillager and the dragon.
		// What was actually wrong with a death was never the death — it was that
		// dying ENDED the event and left the pile lying there. Both of those are
		// fixed where they belong: see claim() and collect() below.

		// Read BEFORE the blow, because the blow ends with him somewhere else.
		// The log printed distanceTo AFTER the backoff teleport, so a hit
		// delivered at arm's length was reported as "swung at 18.1 blocks" —
		// which reads as him hitting through half a field and sent me looking
		// for a reach bug that was never there.
		double reach = this.distanceTo(player);
		this.swipe();
		// HE GOT THERE, AND THAT IS WHAT THE STALEMATE TIMER IS ASKING.
		//
		// It only reset when a blow LANDED, which conflates two different things:
		// "he reached them" and "the damage went through". Armour, a totem, a shield
		// and creative mode all break the second without touching the first — and a
		// creative playtest proved it, because landed=false on every swing meant
		// nothing ever reset the clock and the hunt died of stalemate two minutes
		// after the last punch, mid-fight, with him standing right next to them.
		//
		// Reaching somebody is emphatically not nothing happening. The blow itself
		// still has to land to count as a round; this only says the event is alive.
		this.stalemate = 0;

		// ONE PERSON. Not a swipe that catches whoever is standing about.
		//
		// The vanilla path, the same one an iron golem and a wither skeleton
		// take. doHurtTarget reads ATTACK_DAMAGE, applies the knockback, plays
		// the sound and runs the post-attack effects — all of which the
		// hand-rolled hurtServer call skipped, so even once the cooldown was
		// fixed he would have been hitting for damage with no shove behind it.
		// At SIEGE he stops caring what they are wearing.
		boolean landed;
		if (this.hisGround()) {
			landed = player.hurtServer(here,
				new net.minecraft.world.damagesource.DamageSource(
					here.registryAccess()
						.lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
						.getOrThrow(RECKONING), this),
				RECKONING_DAMAGE);
		} else {
			landed = this.doHurtTarget(here, player);
		}

		// AND THE AXE BURNS.
		//
		// He has carried a diamond axe since the first version and it has been
		// purely cosmetic the whole time — the damage came from ATTACK_DAMAGE and
		// the item in his hand meant nothing. A weapon nobody can feel is set
		// dressing, and four seconds of burning is the cheapest way to make the
		// thing in his hand matter: you back off from the blow already alight,
		// and putting yourself out is one more thing to be doing while he is
		// still there.
		//
		// Only on a blow that LANDED, so armour and a totem still answer it, and
		// only behind the fire switch — somebody who turned fire off did so to
		// keep it out of their world, and their world includes them.
		if (landed && Config.get().scorch) {
			player.igniteForSeconds(AXE_BURNS);
		}

		// AND IT SENDS YOU.
		//
		// doHurtTarget applies vanilla's knockback, which is about four tenths of
		// a block and is calibrated for a zombie. Being hit by him should not
		// feel like being hit by a zombie. This is the one moment in the whole
		// event where he actually touches you, and it should be unmistakable
		// which of you is stronger.
		//
		// Set rather than added, so the direction is his and not the sum of his
		// and vanilla's. hurtMarked is what carries a server-side velocity change
		// to a player's client in 26.2 — without it the server believes they were
		// thrown and their screen shows them standing still, which is the worst
		// of both.
		//
		// The lift is deliberately small next to the shove. A high arc is a long
		// fall and a long fall is fall damage, and "he hit me once and the ground
		// killed me" is a bad death — it reads as the mod misfiring rather than
		// as him. Flat and far is the read: you are somewhere else now, you got
		// there fast, and you are on fire.
		if (landed) {
			Vec3 push = new Vec3(player.getX() - this.getX(), 0.0, player.getZ() - this.getZ());
			if (push.lengthSqr() < 1.0E-4) {
				push = new Vec3(-Math.sin(Math.toRadians(this.getYRot())), 0.0,
					Math.cos(Math.toRadians(this.getYRot())));
			}
			push = push.normalize().scale(AXE_SHOVE);
			player.setDeltaMovement(push.x, AXE_LIFT, push.z);
			player.hurtMarked = true;
		}

		// AND THEN HE IS NOT THERE. He does not stand and trade.
		//
		// This is what makes the phase survivable without slowing him down. He
		// is faster than a sprint by design, so once he arrives the player
		// cannot leave — and a thing that is faster than you AND stays on you
		// is not frightening, it is arithmetic, and the arithmetic says you die
		// in nine seconds every time.
		//
		// So each blow is its own event. He hits once, he is gone before the
		// screen has stopped shaking, and he comes back from somewhere else.
		// The player takes four or five hits across a whole hunt instead of
		// twelve in a row, every one of them lands as a scare rather than as a
		// tick of damage, and the gaps are where they get to do something about
		// it: run, eat, climb, shut a door.
		if (landed) {
			// AND NOW IT IS SOMEBODY ELSE'S TURN.
			//
			// He is finished with this one. Marking them means the next quarry
			// is chosen from whoever has NOT been reached yet, so he works
			// through a group deliberately rather than staying on whoever
			// happens to be nearest — which would let a fast player draw him
			// off their whole party indefinitely.
			//
			// It is also much worse to be on the receiving end of. Being chased
			// is frightening; watching him finish with your friend and turn
			// toward you, and knowing he is going to get to everybody, is a
			// different thing entirely.
			this.struck.add(player.getUUID());
			this.stalemate = 0;
			// He backs off at the END of the window now, in pursue, and only if
			// nobody reached him inside it.
			this.linger = LINGER_MIN + this.random.nextInt(LINGER_SPREAD);
			this.lingerWounded = false;
		}
		// Logged with the answer, not just the attempt. "He is not hitting me"
		// has two completely different causes — he never got in range, or he
		// swung and the damage was refused (creative, invulnerable, a totem) —
		// and they are indistinguishable from the outside.
		HerobrineMod.LOGGER.info("duel: swung at {} blocks from {}, landed={}{}",
			String.format("%.1f", reach), player.getName().getString(), landed,
			player.isCreative() ? " (creative — nothing can land)" : "");
	}


	private void vanish(String why) {
		if (this.bar != null) {
			this.bar.removeAllPlayers();
		}
		HerobrineMod.LOGGER.info("stare over after {} ticks: {}", this.age, why);
		// A HUNT IS OVER, HOWEVER IT ENDED, AND THAT IS WHAT SITES THE CHURCH.
		//
		// Marked here rather than in relent() because relent is only one of the
		// four ways out — three blows, a hundred seconds endured, outrun across
		// a field, or lost down a hole all arrive at this method and all of them
		// are surviving it. Picking a favourite among those would be the mod
		// having an opinion about how they were supposed to play it.
		//
		// Otherwise the half-cracked block keeps its overlay for as long as the
		// chunk stays loaded, which is a very odd souvenir to leave behind.
		if (this.level() instanceof ServerLevel clearing) {
			this.stopBreaking(clearing);
		}
		// SEEN IS THE ONLY CONDITION. It was `witnessed && phase >= TRESPASSER`, and
		// the phase half of that was rationing how much of him you got — which is
		// the job geography has now. Somebody watched him go; there is a mark.
		if (this.witnessed && this.level() instanceof ServerLevel burning) {
			this.scorch(burning);
		}

		// One visit in three takes the light with it, so the departure is not
		// one memorised beat.
		if (this.witnessed && this.random.nextInt(3) == 0
			&& this.level() instanceof ServerLevel lights) {
			Player nearby = lights.getNearestPlayer(this, 24.0);
			if (nearby != null) {
				takeTheLight(lights, nearby);
			}
		}

		if (!this.witnessed) {
			Player missedBy = this.level().getNearestPlayer(this, 96.0);
			if (missedBy instanceof ServerPlayer sp) {
				com.bloomlet.herobrine.manifest.ManifestationDirector.wasted(
					com.bloomlet.herobrine.manifest.Manifestation.THE_STARE, sp);
			}
		}
		if (this.level() instanceof ServerLevel server) {
			double angle = this.random.nextDouble() * Math.PI * 2.0;
			double distance = 14.0 + this.random.nextDouble() * 8.0;
			server.playSound(
				null,
				this.getX() + Math.cos(angle) * distance,
				this.getY(),
				this.getZ() + Math.sin(angle) * distance,
				SoundEvents.STONE_STEP, this.getSoundSource(), 0.3F, 0.9F
			);
		}
		this.discard();
	}


	/**
	 * Nothing touches him.
	 *
	 * He had 40 health and no protection, so the first player to swing a sword
	 * ended the premise — the whole design rests on being unable to fight him
	 * until the Effigy. Damage now does nothing except make him leave, and
	 * leave angrier.
	 *
	 * This is also how the player is taught the rule. Nobody reads a manual:
	 * they hit him, watch it do nothing, and understand. Being told "you
	 * cannot kill this yet" is a worse lesson than finding out.
	 */
	@Override
	public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
		// UNTIL SIEGE, AND ONLY UNTIL SIEGE.
		//
		// Five phases of a thing that cannot be touched is what gives the sixth
		// its weight. A player who has spent forty hours learning that swinging
		// at him does nothing, and then feels a sword actually connect, has been
		// told something no message box could tell them.
		//
		// Environmental damage stays off permanently. He is not to be finished
		// by a cactus, a fall or somebody's lava bucket — this ends with a
		// player hitting him or it does not end.
		if (!(source.getEntity() instanceof ServerPlayer) || !Config.get().theReckoning) {
			return true;
		}
		return Wrath.phase(level.getServer()) != Phase.SIEGE;
	}

	/**
	 * HE DOES NOT BEAT HIMSELF. AND HE HAS BEEN, ALL SESSION.
	 *
	 * isInvulnerableTo already refuses every non-player damage source, so his own
	 * lightning could never take a point of his health and never could. But that
	 * method is not what this one consults — hurtServer OVERRIDES it and does the
	 * whole drive-off accounting BEFORE anything vanilla gets a look in. So the
	 * bolt landed, health was untouched, and thirty lines further down:
	 *
	 *     credit = level.getServer().getPlayerList().getPlayer(this.onThe)
	 *
	 * His own bolt, credited to the person he was hunting, at a quarter rate. Then
	 * the damage floor came off this week and a bolt started being worth five
	 * instead of one, so eight self-inflicted strikes reached forty and he WAS
	 * DRIVEN OFF BY HIS OWN WEATHER. Which reads, correctly, as him killing himself.
	 *
	 * Two rules, in this order, because the order is the whole subtlety:
	 *
	 *   1. Anything with a player behind it is THEIRS, however exotic the delivery.
	 *      A channelling trident is lightning and it is also somebody's idea; TNT
	 *      resolves its indirect source to whoever placed it. Those still count.
	 *   2. Otherwise: his own hand, his own projectiles, and any fire, lightning or
	 *      blast nobody owns. None of it touches the counter.
	 *
	 * Which also closes the cactus, the lava and the long fall — already stated as
	 * the rule in isInvulnerableTo, and only ever enforced for health.
	 */
	private boolean hisOwnDoing(DamageSource source) {
		if (source.getEntity() instanceof ServerPlayer) {
			return false;
		}
		if (source.getEntity() instanceof net.minecraft.world.entity.projectile.Projectile theirs
			&& theirs.getOwner() instanceof ServerPlayer) {
			return false;
		}
		if (source.getEntity() == this || source.getDirectEntity() == this) {
			return true;
		}
		if (source.getEntity() instanceof net.minecraft.world.entity.projectile.Projectile mine
			&& mine.getOwner() == this) {
			return true;
		}
		return source.is(net.minecraft.tags.DamageTypeTags.IS_LIGHTNING)
			|| source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)
			|| source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)
			|| source.is(net.minecraft.tags.DamageTypeTags.IS_FALL);
	}

	/**
	 * He does not burn.
	 *
	 * Not a damage rule — hisOwnDoing covers that — but a LOOK. He sets half a
	 * field alight on purpose and then stands in it, and a figure standing in his
	 * own fire with flames climbing him is the one image in the mod that makes him
	 * look like he made a mistake.
	 */
	@Override
	public boolean fireImmune() {
		return true;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (this.isDying()) {
			return false;      // it is over; nothing lands on a man who is leaving
		}
		if (this.showing) {
			return false;      // not while he is working
		}
		if (this.hisOwnDoing(source)) {
			// Silently. No flinch, no sound, no counter — he does not react to his
			// own weather at all, which is most of what makes it read as his.
			return false;
		}
		// THE RECKONING. He is being killed, and every blow counts the same.
		//
		// The incoming number is thrown away on purpose — see TOTAL_HITS. What
		// a player is swinging decides how the fight LOOKS, never how long it
		// lasts, so the tenth blow is the tenth blow whether it came from a
		// stone sword or a netherite axe.
		// SIEGE ONLY, AND THAT MATTERS MORE THAN IT LOOKS.
		//
		// takeTheBlow is the RECKONING — thirty blows and he is dead for good, and
		// the whole story ends. huntDamage, the forty that drives him off, is a
		// completely separate counter fed from an entirely different branch further
		// down this method.
		//
		// Widening this gate to "any hunt" to let everything hurt him did both of
		// the wrong things at once: player blows stopped reaching the drive-off
		// path, so the bar never moved and forty was unreachable — and the kill
		// counter started running on day one, so thirty hits during any ordinary
		// hunt would have ended the mod.
		//
		// Opening him up belongs on the drive-off branch instead. See below.
		// A PLAYER'S BLOW, IN HIS CASTLE, IS ONE OF THE SEVENTY. Nowhere else is it
		// anything but a reason to be somewhere else — see the attacker branch at
		// the bottom. It used to count at SIEGE in the overworld too, which meant
		// he could be killed on your lawn without the castle ever being found.
		if (source.getEntity() instanceof ServerPlayer striker && this.hisGround()) {
			// THE DAMAGE COMES WITH IT NOW. It was dropped here, which was fine
			// while the blow was only ever counted — the Reckoning is thirty blows
			// whatever they are struck with. The shove is not: see stagger.
			return this.takeTheBlow(level, source, striker, damage);
		}
		// AND IN HIS CASTLE NOTHING ELSE REACHES THE HUNT'S LEDGER. A helper's blow
		// is a quarter of one, and it is never an ending — see helperBlow.
		if (this.hisGround()) {
			return this.helperBlow(level, source, damage);
		}
		// AND THE THREE THAT ARE MEANT TO FIGHT HIM ARE NEVER VAPORISED.
		// Outside a hunt he simply does not feel it; answer() swings back.
		if (challenger(source.getEntity())) {
			this.nowDealWith(source.getEntity());
			// No damage out here — but it has to LAND. A golem swinging at
			// something that does not so much as twitch reads as a broken mob, and
			// the player stops believing the golem is doing anything.
			this.hurtTime = 10;
			this.hurtDuration = 10;
			this.playHurtSound(source);
			return false;
		}
		if (source.getEntity() instanceof net.minecraft.world.entity.Mob other
			&& !com.bloomlet.herobrine.manifest.TheHunt.isHis(other)
			&& other.getTarget() == this) {
			level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
				other.getX(), other.getY() + 0.6, other.getZ(), 16, 0.3, 0.5, 0.3, 0.02);
			other.hurtServer(level, this.damageSources().mobAttack(this), Float.MAX_VALUE);
			HerobrineMod.LOGGER.info("{} put a hand on him and stopped",
				other.getType().toShortString());
			return false;
		}

		// NOT DURING THE OPENING. He is not there to be fought yet — see openOn. A
		// swing landing here used to wound him, count toward the sixty and teleport
		// him off, so the whole routine was cancelled by anybody quick enough to
		// click before he had finished looking at them.

		// SHOOTING HIM SKIPS EVERY OTHER SENSE.
		//
		// He can be crept past, out-crouched and walked around behind, and all of
		// that is the game — but an arrow arriving from sixty blocks in the dark is
		// not something anybody has to notice. There is no line of sight to check
		// and no cone to be inside: somebody has declared themselves, and the only
		// question left is how far away they are standing.
		//
		// It is also the one giveaway that is purely a decision. Everything else he
		// senses can be blamed on bad luck.
		if (this.present && !this.hunting && source.getEntity() instanceof ServerPlayer shooter) {
			HerobrineMod.LOGGER.info("{} shot at him from {} blocks — he does not need to look",
				shooter.getName().getString(), (int) this.distanceTo(shooter));
			this.beginOpening();
			return false;
		}

		if (source.getEntity() instanceof ServerPlayer attacker) {
			// Swinging at him is the loudest possible defiance.
			Heat.noticed(attacker, DEFIANCE_STRUCK);
			// Something is left standing where he was.
			//
			// Swinging at him and having him simply not be there is the correct
			// answer and a slightly empty one — the player gets no
			// acknowledgement that anything happened, and a hit that reads as
			// nothing reads as a bug. Three fires on the spot he was occupying
			// says the swing landed on something, without conceding that it
			// hurt him.
			//
			// Same safeguards as the trespasser scorch, which is why it reuses
			// it rather than lighting its own: never within two blocks of
			// anything flammable, never on burnable ground, and gone after six
			// seconds whatever happens. Take a swing at him indoors and there
			// will be no fire at all, which is the right outcome.
			this.scorch(level, 3);

			// The mid-hunt case is handled far above now, on the branch that takes
			// damage from anything at all. Everything reaching here is out of a
			// hunt, so this is only ever the stare being interrupted.
			// Whoever swung is not necessarily the only one here, so the same
			// all-players check applies before he reappears anywhere.
			if (!relocateBehind(level.getEntitiesOfClass(Player.class,
					this.getBoundingBox().inflate(WATCH_RANGE)))) {
				// Struck rather than merely approached: he leaves the same way,
				// which keeps the two responses consistent.
				this.fleeing = true;
			}
		}
		return false;
	}


	/**
	 * Fireballs, and the rain is genuinely on the player's side.
	 *
	 * SIEGE keeps a storm running permanently, which means anything he sets
	 * alight is being put out from above the whole time. That is not a
	 * concession, it is the design: the phase that makes the world unliveable
	 * is the same phase that stops his fire from taking the world with it, and
	 * a player who works that out has been handed something real by paying
	 * attention.
	 */
	private void throwFire(ServerLevel here, ServerPlayer target, int act) {
		Vec3 from = this.getEyePosition();
		Vec3 to = target.position().add(0.0, 0.9, 0.0).subtract(from);
		float spread = act == 2 ? 5.0F : 2.5F;

		int shots = act == 3 ? 3 : 1;
		for (int i = 0; i < shots; i++) {
			// LARGE, NOT SMALL, AND THIS IS THE WHOLE COMPLAINT ANSWERED.
			//
			// SmallFireball is what a blaze throws: it does five points on a direct
			// hit, sets a small fire, and DOES NOT EXPLODE. So his fireballs could
			// only ever hurt somebody they touched, and they left the ground exactly
			// as they found it — in a dimension whose stated point is that it should
			// look worse every time you come back to it.
			//
			// LargeFireball explodes on impact at the power it is given, which is
			// both the damage and the crater. Power one, a ghast's: enough to be
			// felt and to dish out the floor, and well short of the hole a creeper
			// leaves. HisFireballMixin then hangs a deliberate scorch on the impact
			// on top of that, and it already fires for LargeFireball — so the ground
			// damage the mixin was written for finally applies to HIS shots and not
			// only to the skeletons'.
			net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball ball =
				// POWER BY ACT: one, two, three. Two scratched stone — a ray of intensity
				// two loses 1.9 per stone block, so a castle wall shrugged it off and the
				// fireballs read as fireworks. Three takes a bite out of masonry; the
				// breach below uses four, which is TNT.
				new net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball(
					here, this, to.normalize(), Math.min(3, act));
			ball.snapTo(from.x, from.y, from.z, this.getYRot(), this.getXRot());
			ball.shoot(to.x, to.y, to.z, 1.3F, spread);
			here.addFreshEntity(ball);
		}
		here.playSound(null, this.getX(), this.getY(), this.getZ(),
			SoundEvents.BLAZE_SHOOT, this.getSoundSource(), 1.4F, 0.7F);
	}

	/** A volley, because one arrow from a figure across a field is a joke. */
	private void loose(ServerLevel here, ServerPlayer target, int act) {
		Vec3 from = this.getEyePosition();
		int shots = act == 2 ? 2 : 4;
		float spread = act == 2 ? 8.0F : 4.0F;

		for (int i = 0; i < shots; i++) {
			net.minecraft.world.entity.projectile.arrow.Arrow arrow =
				new net.minecraft.world.entity.projectile.arrow.Arrow(here, this,
					new ItemStack(Items.ARROW), null);
			arrow.snapTo(from.x, from.y - 0.2, from.z, this.getYRot(), this.getXRot());
			double dx = target.getX() - this.getX();
			double dy = target.getY(0.4) - arrow.getY();
			double dz = target.getZ() - this.getZ();
			// The lob, so a volley arcs in rather than arriving flat. It is
			// also what gives the player the second and a half they need to see
			// it coming and get behind something.
			double flat = Math.sqrt(dx * dx + dz * dz);
			arrow.shoot(dx, dy + flat * 0.18, dz, 1.5F, spread);
			arrow.setBaseDamage(3.0);
			here.addFreshEntity(arrow);
		}
		here.playSound(null, this.getX(), this.getY(), this.getZ(),
			SoundEvents.SKELETON_SHOOT, this.getSoundSource(), 1.2F, 0.6F);
	}

	/**
	 * Act three: the sky, on their roof.
	 *
	 * Aimed at the ground AROUND the player rather than at the player, and
	 * visual-only, with the ordinary scorch safeguards behind it. A real bolt
	 * would do six damage through armour on top of everything else in act
	 * three and would burn a wooden house to the foundations while the player
	 * was standing in it — which is a lost world, not a lost fight, and no
	 * amount of spectacle is worth it.
	 *
	 * The fire it leaves is the threat. The rain is already taking it back out.
	 */
	/** How wide he misses by, per act. He gets better. */
	private static final double[] AIM_OFF = { 5.5, 3.5, 1.5 };

	/** How much of your own speed he throws ahead of you. He learns. */
	private static final double[] LEADS_BY = { 0.0, 0.5, 1.0 };

	/** How long the ground shows where it is going to land. */
	private static final int WINDS_UP = 22;

	/**
	 * HE THROWS IT. It is not weather any more.
	 *
	 * This scattered bolts round the player at a random bearing and let a third of
	 * them bite, which reads as a storm taking an interest — atmospheric, and
	 * nothing to do with him. Asked for the other thing: he throws it, the way Thor
	 * does, and of course he misses a bit, and if it lands on you it hurts.
	 *
	 * THE MISS IS EARNED, NOT ROLLED. He aims at where the player is standing AT
	 * THE MOMENT HE THROWS, marks that spot, and the bolt arrives WINDS_UP ticks
	 * later. Stand still and it lands on your head for the full five points and the
	 * fire. Move and he has thrown it at a patch of ground you used to be on.
	 *
	 * That is worth more than any random chance to miss. A dice roll teaches
	 * nothing; a second of warning teaches you to keep moving, and a player who has
	 * learned that has been taught it by the fight rather than by a wiki. AIM_OFF
	 * then closes from five and a half blocks to one and a half across the three
	 * acts, so the lesson stops being enough.
	 *
	 * The flanking bolts stay visual-only. They are the spectacle and they are
	 * deliberately harmless — the danger has to be in the one place the player was
	 * shown, or the warning means nothing.
	 */
	private void callDown(ServerLevel here, ServerPlayer target) {
		boolean real = Config.get().realLightning;
		int act = this.act();
		double off = AIM_OFF[Math.min(AIM_OFF.length - 1, act - 1)];

		// ---- THE THROW. Where he is aiming, decided now.
		//
		// AND HE LEARNS TO LEAD. Aiming at where somebody IS, with a second of
		// warning, is a shot that lands on people who stand still and on nobody
		// else — simulated, act three was a hundred per cent against a standing
		// player and exactly zero against a walking one. Which teaches the lesson
		// once and then never matters again.
		//
		// So the aim point drifts along the player's own velocity, none of it in
		// act one and all of it in act three. Now standing still is death, walking
		// in a straight line is death by act three, and the thing that saves you is
		// CHANGING DIRECTION — which is a harder lesson, learnable, and the right
		// one for the last act of the fight.
		double lead = LEADS_BY[Math.min(LEADS_BY.length - 1, act - 1)];
		Vec3 going = target.getDeltaMovement().scale(WINDS_UP * lead);
		double angle = this.random.nextDouble() * Math.PI * 2.0;
		double slip = Math.sqrt(this.random.nextDouble()) * off;
		final double ax = target.getX() + going.x + Math.cos(angle) * slip;
		final double az = target.getZ() + going.z + Math.sin(angle) * slip;
		final int ay = here.getHeight(
			net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
			net.minecraft.util.Mth.floor(ax), net.minecraft.util.Mth.floor(az));

		this.getLookControl().setLookAt(target, 60.0F, 60.0F);
		this.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
		here.playSound(null, this.getX(), this.getY(), this.getZ(),
			SoundEvents.TRIDENT_THUNDER.value(), this.getSoundSource(), 2.0F, 1.6F);

		// The mark, ticking on the ground for a second. This is the whole fairness
		// of the move: the player is TOLD, in the world, with time to walk out.
		for (int t = 0; t < WINDS_UP; t += 2) {
			com.bloomlet.herobrine.manifest.Cadence.in(here.getServer(), t, () -> {
				here.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
					ax, ay + 0.2, az, 12, 1.1, 0.1, 1.1, 0.02);
				here.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
					ax, ay + 0.1, az, 4, 0.9, 0.05, 0.9, 0.0);
			});
		}

		com.bloomlet.herobrine.manifest.Cadence.in(here.getServer(), WINDS_UP, () -> {
			net.minecraft.world.entity.LightningBolt bolt =
				net.minecraft.world.entity.EntityTypes.LIGHTNING_BOLT
					.create(here, net.minecraft.world.entity.EntitySpawnReason.EVENT);
			if (bolt == null) {
				return;
			}
			bolt.setVisualOnly(!real);
			bolt.snapTo(ax, ay, az, 0.0F, 0.0F);
			here.addFreshEntity(bolt);
		});

		// ---- AND THE SHOW, well out of the way and harmless.
		int flanking = 2 + this.random.nextInt(3);
		for (int i = 0; i < flanking; i++) {
			double a2 = this.random.nextDouble() * Math.PI * 2.0;
			double r2 = 14.0 + this.random.nextDouble() * 16.0;
			final double x = target.getX() + Math.cos(a2) * r2;
			final double z = target.getZ() + Math.sin(a2) * r2;
			final int y = here.getHeight(
				net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
				net.minecraft.util.Mth.floor(x), net.minecraft.util.Mth.floor(z));
			com.bloomlet.herobrine.manifest.Cadence.in(here.getServer(),
					WINDS_UP - 4 + this.random.nextInt(10), () -> {
				net.minecraft.world.entity.LightningBolt flash =
					net.minecraft.world.entity.EntityTypes.LIGHTNING_BOLT
						.create(here, net.minecraft.world.entity.EntitySpawnReason.EVENT);
				if (flash == null) {
					return;
				}
				flash.setVisualOnly(true);
				flash.snapTo(x, y, z, 0.0F, 0.0F);
				here.addFreshEntity(flash);
			});
		}
		this.scorch(here, 3);
	}

	/**
	 * One blow of thirty.
	 *
	 * He does not relocate, does not flee and does not vanish. That is the
	 * whole difference between this and every other time a player has swung at
	 * him: for five phases the answer to a sword was that he was somewhere else
	 * by the time it arrived, and here he simply stands and takes it and gets
	 * worse.
	 */
	/**
	 * A HELPER'S BLOW, IN HIS CASTLE.
	 *
	 * Addexio hits him for nine. That used to go through the hunt's damage ledger
	 * at a quarter, and forty points of that ledger is the hunt ENDING — "driven
	 * off", back through the door, a fresh one of him over the keep with the count
	 * at zero. The log said "act 2" twice in one fight and that was why: not two
	 * systems pulling at once, two systems with different ideas of the finish.
	 *
	 * So in here a helper's blow is worth what a hand on him is worth divided by
	 * four — every fourth one is a blow of the seventy, credited to the nearest
	 * player — and it touches nothing else. Addexio matters; Addexio cannot end
	 * it; and nothing but the count can send him anywhere.
	 */
	private static final int HELPER_BLOWS_PER = 4;

	private net.minecraft.world.entity.@org.jspecify.annotations.Nullable Mob lastHelper;
	private long lastHelperAt;

	private boolean helperBlow(ServerLevel level, DamageSource source, float damage) {
		if (!(source.getEntity() instanceof net.minecraft.world.entity.Mob other)
			|| com.bloomlet.herobrine.manifest.TheHunt.isHis(other) || damage <= 0.0F) {
			return false;
		}
		this.lastHelper = other;
		this.lastHelperAt = level.getGameTime();
		this.hurtTime = 10;
		this.hurtDuration = 10;
		this.playHurtSound(source);
		if (++this.helperBlows % HELPER_BLOWS_PER == 0
			&& level.getNearestPlayer(this, 48.0) instanceof ServerPlayer credit) {
			this.takeTheBlow(level, source, credit, damage);
		}
		return false;
	}

	/**
	 * What a save, or a fresh one of him, is told about the fight so far.
	 *
	 * The Ender Dragon does not heal because you went to bed. Neither does he: the
	 * count and the binding live on his level, and whichever of him comes to stand
	 * over the keep next picks them up on the way in. Once per entity, or
	 * loseInterest would re-arm the hunt every time it put him back to prowling.
	 */
	private void recall() {
		if (this.recalled || !(this.level() instanceof ServerLevel his) || !this.hisGround()) {
			return;
		}
		this.recalled = true;
		int had = com.bloomlet.herobrine.manifest.Reckoning.hits(his);
		boolean was = com.bloomlet.herobrine.manifest.Reckoning.bound(his);
		if (had <= 0 && !was) {
			return;
		}
		this.hits = had;
		this.bound = was;
		this.partyPeak = Math.max(1, com.bloomlet.herobrine.manifest.Reckoning.party(his));
		this.hunting = true;
		this.wearTheAct();
		this.setHealth(Math.max(1.0F, this.blowsNeeded() - this.hits));
		HerobrineMod.LOGGER.info("he remembers: {} blows taken, bound={}", had, was);
	}

	private void announceBlink(ServerLevel where, double x, double y, double z) {
		where.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
			x, y + 1.0, z, 24, 0.35, 0.7, 0.35, 0.02);
		where.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
			x, y + 1.0, z, 10, 0.3, 0.6, 0.3, 0.01);
		where.playSound(null, x, y, z, SoundEvents.ENDERMAN_TELEPORT,
			this.getSoundSource(), 1.4F, 0.45F);
	}

	/**
	 * ONE BLOW PER HALF SECOND, PER HAND.
	 *
	 * Vanilla lets every click through: the attack-cooldown only scales the DAMAGE
	 * down, it does not stop hurtServer being called, and vanilla's own immunity
	 * frames live in the super.hurtServer this override never reaches. So a player
	 * mashing the button landed four "blows" a second — the log showed act three at
	 * 46 and him dead six seconds later — and the last fight in the mod was a
	 * clicking contest. A sword's honest rhythm is 1.6 a second; a blow counts if
	 * it is at least ten ticks after that player's last counted one. The click
	 * still connects (sound, flash, stagger); it just is not one of the seventy.
	 */
	private static final long BLOW_SPACING = 10L;
	private final java.util.Map<java.util.UUID, Long> lastCounted = new java.util.HashMap<>();

	private boolean takeTheBlow(ServerLevel level, DamageSource source, ServerPlayer striker,
	                            float damage) {
		long now = level.getGameTime();
		Long last = this.lastCounted.get(striker.getUUID());
		if (last != null && now - last < BLOW_SPACING) {
			this.hurtTime = 10;
			this.hurtDuration = 10;
			this.stagger(striker, damage * 0.25F);
			return true;      // connected, not counted
		}
		this.lastCounted.put(striker.getUUID(), now);
		int was = this.act();
		this.hits++;
		this.lastStruckBy = striker.getUUID();
		this.noteStriker(striker, level.getGameTime());
		// THE FIRST BLOW TAKES THE SKY FROM HIM. Only in his castle — the overworld
		// siege is still the hunt's fight, and the hunt needs the air for walls.
		if (this.hisGround()) {
			this.bind();
			com.bloomlet.herobrine.manifest.Reckoning.record(level, this.hits, this.bound,
				this.partyPeak);
		}
		this.hunting = true;      // whatever he was doing, he is doing this now
		this.relenting = false;
		// AND THE LATCH TOO. Without this a hunt he had already broken off would
		// leave brokenOff set, the branch above would skip closeOn for the whole
		// Reckoning, and the last fight in the mod would be a figure that never
		// swung at anybody.
		this.brokenOff = false;
		this.watching = false;
		if (this.flying) {
			this.land();   // the pause put him up; its ending takes him down
		}
		this.struck.clear();

		Heat.noticed(striker, DEFIANCE_STRUCK);
		this.anger(level);
		// The Reckoning gets the same shove as the hunt. Same reason, same
		// numbers — see stagger. It is read off the source's own damage so a
		// netherite sword still feels different from a fist in the last fight.
		this.stagger(striker, damage);

		if (this.hits >= this.blowsNeeded()) {
			this.beginDying(level, striker);
			return true;
		}
		this.wearTheAct();
		if (this.hits == Math.max(1, this.blowsNeeded() / 3)) {
			com.bloomlet.herobrine.manifest.Reckoning.theWarning(level, striker, this);
		}
		// One point of the health bar per blow, which is why MAX_HEALTH is the
		// hit count rather than a number of hearts. The bar is the honest
		// progress meter and it is the only one the player gets.
		this.setHealth(Math.max(1.0F, this.blowsNeeded() - this.hits));
		this.hurtTime = 10;
		this.hurtDuration = 10;
		return true;
	}

	/**
	 * He gets worse, and it has to be visible without a new texture.
	 *
	 * The enderman note was the right reference: what makes one frightening
	 * when provoked is that it visibly changes while doing nothing else
	 * differently. So the escalation is particles and fire, both scaling with
	 * the count, because those cost nothing to add and — unlike a colour on the
	 * model — cannot crash a client if the mixin selector is wrong, which has
	 * already happened twice on this project.
	 */
	private void anger(ServerLevel level) {
		int stage = 1 + this.hits / THE_WARNING;
		level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
			this.getX(), this.getY() + 1.1, this.getZ(),
			12 * stage, 0.45, 0.7, 0.45, 0.02);
		level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
			this.getX(), this.getY() + 1.0, this.getZ(),
			4 * stage, 0.4, 0.6, 0.4, 0.01);
		// AND IT ROLLS AWAY ACROSS THE COUNTRY. Volume three rather than one,
		// because one is sixteen blocks and the loudest thing in the mod could
		// not be heard from the far side of a field — and then it comes back off
		// the hills a beat later. See ModSounds.roll.
		com.bloomlet.herobrine.sound.ModSounds.roll(level, this.blockPosition(),
			com.bloomlet.herobrine.sound.ModSounds.ANGER,
			3.0F, 1.06F - stage * 0.06F);
		// More of it every act, and still refused wherever it would spread.
		this.scorch(level, stage * 2);
	}

	/**
	 * Chase him and he is behind you.
	 *
	 * Vanishing when approached was a resolution, and resolutions end fear —
	 * you walked at him, he dissolved, you won. Going back to where you were
	 * standing when he arrived turns the chase itself into the scare: you
	 * closed the distance for nothing, and the ground you gave up is now
	 * occupied.
	 *
	 * No effect, no sound, no motion. You never see him move — you turn round
	 * and he is simply at the other end, which is the same rule that governs
	 * his arrival.
	 *
	 * @return false when he has run out of relocations or there is nowhere
	 *         valid, in which case the caller makes him leave for good.
	 */
	private boolean relocateBehind(List<Player> watchers) {
		if (this.relocations >= MAX_RELOCATIONS
			|| this.anchor == null
			|| !(this.level() instanceof ServerLevel server)) {
			return false;
		}
		// ONE IN TWO, ALWAYS. This used to climb from a third to a certainty across
		// the ladder; the ladder no longer describes him. Half the time he is
		// somewhere else a moment later and half the time he is simply gone, which is
		// the whole of what an apparition needs to do.
		if (this.random.nextInt(2) != 0) {
			return false;   // this time he simply goes
		}
		// The anchor may have been mined out, flooded, or built over since.
		if (!ConfinedPlacement.canStand(server, this.anchor)) {
			return false;
		}
		// Clear of everybody, not just the one who walked him down. Dropping
		// him behind the player who charged is worthless if it puts him in
		// their friend's face.
		for (Player watcher : watchers) {
			if (this.anchor.distToCenterSqr(watcher.getX(), watcher.getY(), watcher.getZ())
				< TOO_CLOSE * TOO_CLOSE) {
				return false;   // too near one of them; it would look like a stutter
			}
		}

		// Facing whoever is nearest the place he reappears, so he is looking at
		// somebody rather than off into the trees.
		Player facing = watchers.get(0);
		double best = Double.MAX_VALUE;
		for (Player watcher : watchers) {
			double distance = this.anchor.distToCenterSqr(
				watcher.getX(), watcher.getY(), watcher.getZ());
			if (distance < best) {
				best = distance;
				facing = watcher;
			}
		}
		double dx = facing.getX() - (this.anchor.getX() + 0.5);
		double dz = facing.getZ() - (this.anchor.getZ() + 0.5);
		float yaw = (float)(net.minecraft.util.Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
		// Through blink, not snapTo. This one is the whole "chase him and he is
		// behind you" trick, and it was the most visible instance of the streak:
		// the player charges him, and instead of finding empty ground they watch
		// him slide back past their shoulder to where they started.
		this.blink(this.anchor.getX() + 0.5, this.anchor.getY(), this.anchor.getZ() + 0.5, yaw);

		this.relocations++;
		this.unseenTicks = 0;
		// The clock back to zero, because this is a new sighting. Without it the
		// second appearance is instant and the player walks round the rock to
		// find him already gone.
		this.watchedTicks = 0;
		this.age = 0;          // a fresh visit; you have earned the second look
		return true;
	}

	/** Silent by design — no idle noise to give away where he is standing. */
	@Override
	public boolean isSilent() {
		return true;
	}

	/**
	 * NOT DURING A HUNT. THIS WAS ENDING THEM SILENTLY.
	 *
	 * Returning true unconditionally hands him to vanilla's despawn sweep, which
	 * removes any mob that gets far enough from a player — no event, no log, no
	 * "stare over", nothing. A playtest shows exactly what that costs: he sends a
	 * wave, withdraws to twenty blocks, the player runs off fighting the seven
	 * villagers, and somewhere in the next ninety seconds he is quietly deleted.
	 * The ladder stops mid-sequence, the wave gate never gets to notice the wave is
	 * dead, and the last line in the log is unrelated. From inside the game the
	 * hunt simply stops meaning anything.
	 *
	 * TRUE IS RIGHT FOR THE STARE and wrong for everything else. A figure at the
	 * treeline is meant to be ephemeral — walk away and he was never there. A hunt
	 * is the opposite promise: it ends on its own terms, and it has four of them
	 * already. Outrun him by eighty-eight blocks for three seconds and he gives up,
	 * loudly. Break his line of sight for twenty to sixty seconds and he loses you.
	 * Two minutes where nobody touches anybody and he goes. Sixty damage and he is
	 * driven off. Every one of those is a decision somebody can perceive.
	 *
	 * Vanilla's radius is not a fifth exit. It is an accident of chunk loading.
	 */
	@Override
	public boolean removeWhenFarAway(double distanceSquared) {
		return !this.hunting && !this.present;
	}

	// ---- THE DUEL'S HANDS ON HIM ------------------------------------------------
	//
	// Duel lives in its own file because this one is eight thousand lines, and
	// these are the only things it is allowed to touch. Each is a name for one
	// private primitive above, so the fight can be read in one place without the
	// primitives becoming public to the world.

	boolean isBound() {
		return this.bound;
	}

	boolean isDying() {
		return this.getAttached(DYING_SINCE) != null;
	}

	/**
	 * THE END, THE WAY THE DRAGON ENDS.
	 *
	 * He used to fall over. Seventy — now a hundred — blows, and the last one made
	 * him lie down and vanish like a zombie, with the bar still on the screen. The
	 * Ender Dragon rises, whitens, and bursts, and every player knows that shape
	 * means the thing is over. So: for seven seconds nothing can touch him, he
	 * lifts off the floor, grows to nearly three times his size, goes white (see
	 * HerobrineRenderer.getWhiteOverlayProgress), the light pours out of him — and
	 * then die() runs, with everything it already did: the morning, the loot at the
	 * killer's feet, the song, the patch note on the screen.
	 */
	public static final net.fabricmc.fabric.api.attachment.v1.AttachmentType<Long> DYING_SINCE =
		net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry.<Long>builder()
			.syncWith(net.minecraft.network.codec.ByteBufCodecs.VAR_LONG.cast(),
				net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate.all())
			.buildAndRegister(HerobrineMod.id("dying_since"));
	public static final int DYING_TAKES = 140;
	private static final double DYING_GROWS_TO = 2.8;
	private java.util.@org.jspecify.annotations.Nullable UUID dyingStruckBy;
	private double dyingScaleFrom = 1.0;
	private int dyingFor;

	private void beginDying(ServerLevel level, ServerPlayer striker) {
		com.bloomlet.herobrine.manifest.Reckoning.clear(level);
		this.setAttached(DYING_SINCE, level.getGameTime());
		this.dyingStruckBy = striker.getUUID();
		this.dyingFor = 0;
		var size = this.getAttribute(Attributes.SCALE);
		this.dyingScaleFrom = size == null ? 1.0 : size.getBaseValue();
		this.getNavigation().stop();
		this.setDeltaMovement(Vec3.ZERO);
		this.setNoGravity(true);
		this.setInvulnerable(true);
		this.setRemainingFireTicks(0);
		level.playSound(null, this.getX(), this.getY(), this.getZ(),
			SoundEvents.SOUL_ESCAPE.value(), this.getSoundSource(), 3.0F, 0.4F);
		com.bloomlet.herobrine.sound.ModSounds.roll(level, this.blockPosition(),
			com.bloomlet.herobrine.sound.ModSounds.ANGER, 4.0F, 0.5F);
		HerobrineMod.LOGGER.info("the last blow — he is leaving, {} ticks", DYING_TAKES);
	}

	private void dyingTick(ServerLevel here) {
		this.dyingFor++;
		double t = Math.min(1.0, this.dyingFor / (double) DYING_TAKES);
		this.setNoGravity(true);
		this.setDeltaMovement(0.0, 0.045, 0.0);
		this.hurtMarked = true;
		var size = this.getAttribute(Attributes.SCALE);
		if (size != null) {
			size.setBaseValue(this.dyingScaleFrom + (DYING_GROWS_TO - this.dyingScaleFrom) * t);
		}
		double h = this.getBbHeight();
		here.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
			this.getX(), this.getY() + h * 0.5, this.getZ(), 3, 0.6 * t + 0.3, h * 0.4, 0.6 * t + 0.3, 0.02);
		if (this.dyingFor % 20 == 0) {
			here.sendParticles(net.minecraft.core.particles.ColorParticleOption.create(
					net.minecraft.core.particles.ParticleTypes.FLASH, 0xFFFFFFFF),
				this.getX(), this.getY() + h * 0.5, this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
			here.playSound(null, this.getX(), this.getY(), this.getZ(),
				SoundEvents.SOUL_ESCAPE.value(), this.getSoundSource(), 2.0F, 0.5F + (float) t);
		}
		if (this.dyingFor >= DYING_TAKES) {
			here.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,
				this.getX(), this.getY() + h * 0.5, this.getZ(), 2, 0.0, 0.0, 0.0, 0.0);
			ServerPlayer killer = this.dyingStruckBy == null ? null
				: here.getServer().getPlayerList().getPlayer(this.dyingStruckBy);
			DamageSource source = killer != null
				? this.damageSources().playerAttack(killer) : this.damageSources().generic();
			this.setInvulnerable(false);
			this.removeAttached(DYING_SINCE);
			super.hurtServer(here, source, Float.MAX_VALUE);
		}
	}

	boolean inTheAir() {
		return this.flying;
	}

	int hitsTaken() {
		return this.hits;
	}

	int actNow() {
		return this.act();
	}

	java.util.@org.jspecify.annotations.Nullable UUID lastStruckBy() {
		return this.lastStruckBy;
	}

	void down() {
		this.land();
	}

	void noStalemate() {
		this.stalemate = 0;
	}

	/** Whatever mood the hunt left him in, he is in a fight now. */
	void engaged() {
		this.hunting = true;
		this.relenting = false;
		this.brokenOff = false;
		this.watching = false;
		this.fleeing = false;
	}

	void face(net.minecraft.world.entity.Entity foe) {
		this.squareUp(foe);
	}

	void slash(ServerPlayer player) {
		this.strike(player);
	}

	/**
	 * A HELPER THAT NEEDS DEALING WITH. The one that hit him in the last ten seconds
	 * if it is still within six blocks, else anything within four that has him as
	 * its target — a golem walking up to swing. Never one of his own.
	 */
	net.minecraft.world.entity.@org.jspecify.annotations.Nullable Mob helperToDealWith() {
		if (!(this.level() instanceof ServerLevel here)) {
			return null;
		}
		long now = here.getGameTime();
		if (this.lastHelper != null && this.lastHelper.isAlive() && now - this.lastHelperAt < 200
			&& this.distanceTo(this.lastHelper) < 6.0) {
			return this.lastHelper;
		}
		this.lastHelper = null;
		for (net.minecraft.world.entity.Mob mob : here.getEntitiesOfClass(
				net.minecraft.world.entity.Mob.class, this.getBoundingBox().inflate(4.0),
				m -> m.isAlive() && m.getTarget() == this
					&& !com.bloomlet.herobrine.manifest.TheHunt.isHis(m))) {
			return mob;
		}
		return null;
	}

	/**
	 * NOTHING OUT HERE IS GOING TO HELP YOU. He turns, and it is over: the same
	 * sentence the overworld apparition says to anything that puts a hand on him,
	 * said in his castle to the golems somebody brought. Four golems bought seventy
	 * blows in seventy seconds from behind a pillar; now they buy a few seconds and
	 * a couple of blows, and the player watches what bringing them cost. Addexio is
	 * the exception by construction — his hurtServer will not let him below two
	 * hearts, so this puts him to flight instead of in the ground.
	 */
	void breakHelper(net.minecraft.world.entity.Mob helper) {
		if (!(this.level() instanceof ServerLevel here)) {
			return;
		}
		this.squareUp(helper);
		this.swipe();
		here.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
			helper.getX(), helper.getY() + helper.getBbHeight() * 0.5, helper.getZ(),
			24, 0.4, 0.6, 0.4, 0.02);
		here.playSound(null, helper.getX(), helper.getY(), helper.getZ(),
			SoundEvents.PLAYER_ATTACK_CRIT, this.getSoundSource(), 1.4F, 0.5F);
		// A golem ends in one. Addexio is a man in diamond with twenty-four hearts and
		// the only one whose death this fight can cause — he gets a blow, the real
		// one, and it takes several. Long enough for the player to notice and decide.
		if (helper instanceof CompanionEntity) {
			helper.hurtServer(here, this.damageSources().mobAttack(this), RECKONING_DAMAGE);
		} else {
			helper.hurtServer(here, this.damageSources().mobAttack(this), Float.MAX_VALUE);
		}
		HerobrineMod.LOGGER.info("duel: {} put a hand on him and {}",
			helper.getType().toShortString(), helper.isAlive() ? "paid for it" : "stopped");
	}

	/**
	 * THERE ARE BLOCKS BETWEEN US. HE REMOVES THEM.
	 *
	 * Two things a player does that a fight should not reward: wall themselves in,
	 * and pillar up. Both put blocks between him and them where a blink has no
	 * room to land, and he stood there. Now the blocks go: two fireballs at power
	 * two, at the first block on the line from his eye to theirs — or, if they are
	 * above him with nothing between but air and height, at the block under their
	 * feet. Gated on breakIn and on mobGriefing like every other hole he makes.
	 */
	private static final double BREACH_REACH = 24.0;
	/**
	 * A BREACH BALL CARVES. Vanilla's blast is a poor mason: a ray loses 1.9 for
	 * every step through stone, so even TNT takes one block out of a wall and none
	 * out of the next. The two balls he fires at a wall carry this mark, and on
	 * impact HisFireballMixin punches a hole through whatever they hit — see
	 * HisHost.punch. Not synced, not saved: a fireball lives two seconds.
	 */
	public static final net.fabricmc.fabric.api.attachment.v1.AttachmentType<Boolean> BREACH =
		net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry.create(HerobrineMod.id("breach"));
	/** The block he aimed the breach at, so the hole is there even if the ball hits short. */
	public static final net.fabricmc.fabric.api.attachment.v1.AttachmentType<Long> BREACH_AT =
		net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry.create(HerobrineMod.id("breach_at"));

	boolean breach(ServerPlayer target) {
		if (!(this.level() instanceof ServerLevel here) || !Config.get().breakIn
			|| !here.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING)
			|| this.distanceTo(target) > BREACH_REACH) {
			return false;
		}
		Vec3 eye = this.getEyePosition();
		Vec3 theirs = target.getEyePosition();
		net.minecraft.world.phys.BlockHitResult hit = here.clip(
			new net.minecraft.world.level.ClipContext(eye, theirs,
				net.minecraft.world.level.ClipContext.Block.COLLIDER,
				net.minecraft.world.level.ClipContext.Fluid.NONE, this));
		BlockPos wall;
		if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
			wall = hit.getBlockPos();
		} else if (target.getY() - this.getY() > 3.0 && target.onGround()) {
			wall = target.blockPosition().below();
		} else {
			return false;
		}
		Vec3 at = Vec3.atCenterOf(wall);
		Vec3 along = at.subtract(eye);
		if (along.lengthSqr() < 1.0) {
			return false;
		}
		this.swipe();
		// TNT-CLASS FROM ACT TWO. Power two did nothing to a stone wall — see
		// throwFire — so "he blows the place up" was a puff of smoke and a dent in
		// the floor. Four goes through a block and a half of masonry.
		int power = this.act() >= 2 ? 4 : 3;
		for (int i = 0; i < 2; i++) {
			net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball ball =
				new net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball(
					here, this, along.normalize(), power);
			ball.snapTo(eye.x, eye.y, eye.z, this.getYRot(), this.getXRot());
			ball.shoot(along.x, along.y, along.z, 1.4F, 0.6F);
			ball.setAttached(BREACH, true);
			ball.setAttached(BREACH_AT, wall.asLong());
			here.addFreshEntity(ball);
		}
		here.playSound(null, this.getX(), this.getY(), this.getZ(),
			SoundEvents.BLAZE_SHOOT, this.getSoundSource(), 1.6F, 0.5F);
		HerobrineMod.LOGGER.info("duel: blocks between them — fire at [{}, {}, {}]",
			wall.getX(), wall.getY(), wall.getZ());
		return true;
	}

	/** Distinct players who have struck him inside the last minute; never below one. */
	int partyNow() {
		long now = this.level().getGameTime();
		this.strikers.values().removeIf(at -> now - at > PARTY_WINDOW);
		return Math.max(1, this.strikers.size());
	}

	/**
	 * THE SWEEP. Two or more of them at arm's length is not a duel, it is a mob
	 * holding him down — and the answer is one swing that reaches all of them:
	 * half a blow's damage to everyone within three and a half blocks, and a shove
	 * apart. It is what makes standing together on him a mistake rather than a
	 * strategy. Returns how many it caught.
	 */
	private static final double SWEEP_ARC = 3.5;

	int sweepThemOff() {
		if (!(this.level() instanceof ServerLevel here)) {
			return 0;
		}
		this.swipe();
		int caught = 0;
		net.minecraft.world.damagesource.DamageSource source =
			new net.minecraft.world.damagesource.DamageSource(here.registryAccess()
				.lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
				.getOrThrow(RECKONING), this);
		for (ServerPlayer near : here.getEntitiesOfClass(ServerPlayer.class,
				this.getBoundingBox().inflate(SWEEP_ARC), p -> p.isAlive() && !p.isSpectator())) {
			if (near.hurtServer(here, source, RECKONING_DAMAGE * 0.5F)) {
				caught++;
			}
			Vec3 push = new Vec3(near.getX() - this.getX(), 0.0, near.getZ() - this.getZ());
			if (push.lengthSqr() < 1.0E-4) {
				push = near.getViewVector(1.0F).scale(-1.0);
			}
			push = push.normalize().scale(1.2);
			near.setDeltaMovement(push.x, 0.35, push.z);
			near.hurtMarked = true;
		}
		here.sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK,
			this.getX(), this.getY() + 1.2, this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
		here.playSound(null, this.getX(), this.getY(), this.getZ(),
			SoundEvents.PLAYER_ATTACK_SWEEP, this.getSoundSource(), 1.3F, 0.6F);
		return caught;
	}

	void fire(ServerLevel here, ServerPlayer target, int act) {
		this.throwFire(here, target, act);
	}

	void volley(ServerLevel here, ServerPlayer target, int act) {
		this.loose(here, target, act);
	}

	void bolt(ServerLevel here, ServerPlayer target) {
		this.callDown(here, target);
	}

	boolean appearAt(Player quarry, double min, double max, boolean wantSeen) {
		return this.reappearAt(quarry, min, max, wantSeen);
	}

	boolean beside(Player quarry) {
		return this.alongside(quarry);
	}

	boolean upTo(Player quarry) {
		return this.closeIn(quarry);
	}

	boolean theyAreAloft(Player quarry) {
		return this.aloft(quarry);
	}

	/** Loosely in front of them with a clear line — being LOOKED AT, not merely visible. */
	boolean inTheirView(Player quarry) {
		return this.inViewOf(quarry);
	}

	boolean seesSpot(Player quarry, BlockPos at) {
		return this.level() instanceof ServerLevel here && clearTo(here, quarry, at);
	}

	void blinkTo(double x, double y, double z, float yaw) {
		this.blink(x, y, z, yaw);
	}

	/** The entrance orbit, for the Duel's first stage. False if there is nothing to circle. */
	boolean circleTheKeep() {
		return this.patrol();
	}

	/**
	 * The sky is taken from him. Once; by the first blow he takes in his castle, or
	 * by the first one he gives when you stood in his hall too long.
	 */
	void bind() {
		if (this.bound) {
			return;
		}
		this.bound = true;
		this.hunting = true;
		this.land();
		if (this.level() instanceof ServerLevel his && this.hisGround()) {
			com.bloomlet.herobrine.manifest.Reckoning.record(his, this.hits, true, this.partyPeak);
		}
		HerobrineMod.LOGGER.info("the first blow — his feet are on the ground for the rest of this");
	}
}
