#!/usr/bin/env python3
"""His voice, synthesised.

EVERY SOUND IN THIS MOD IS BORROWED, and six of them are the warden's. The
warden is one of the three most recognisable noises in the game — a player who
hears it does not think "him", they think "warden", and the entire claim this
mod makes is that he is a PERSON rather than a monster. Borrowing a monster's
throat to say so was working against the writing.

So these are ours. Written rather than recorded, because a synthesiser can be
kept in a repository and a microphone cannot: no licence, no attribution, no
binary somebody has to trust, and the whole set regenerates from this file.

WHAT SYNTHESIS IS GOOD AT, and this deliberately stays inside it: low drones,
sub-bass swells, breath, heartbeats, wind, room tone. What it is bad at is a
voice or an organic growl, so nothing here attempts one. That limit turns out to
suit him — everything he does in this mod is pressure, weather and absence, and
none of it is a roar.

Pure stdlib, then ffmpeg to Ogg Vorbis, which is the only format Minecraft
takes. No numpy: a few seconds of mono at 44.1k is a couple of hundred thousand
floats and Python does that in under a second.

Run:  python3 tools/gen_sounds.py
"""
import math
import os
import random
import struct
import subprocess
import sys
import wave

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, '..', 'src', 'main', 'resources',
                   'assets', 'herobrine', 'sounds')
RATE = 44100


# ---- the toolkit -------------------------------------------------------
#
# WHY THIS IS NOT A WARDEN REMIX, which was the obvious suggestion and is the
# wrong answer for a reason that has nothing to do with craft: Minecraft's sound
# files are Mojang's. Shipping a pitched-down, reversed, filtered warden is still
# shipping Mojang's asset, and a mod that redistributes them is a mod that cannot
# be published. Processing does not launder ownership.
#
# What CAN be taken is the recipe. What makes the warden — and every good deep
# sound — work is not its waveform, it is four techniques, and all four are
# arithmetic anybody can write:
#
#   FM, which is where the character lives. A sine modulating another sine's
#     frequency produces INHARMONIC partials — sidebands that are not whole
#     multiples of the fundamental — and inharmonic is the entire difference
#     between "a tone" and "a throat". Every organic-sounding synth voice in
#     games is doing this. It is also why the first pass sounded thin: it was
#     stacked sines, which is a church organ.
#
#   RESONANCE. A two-pole filter with feedback rings at its cutoff, and a
#     ringing filter is a body — a chest, a room, a pipe. Sweeping the cutoff
#     under a static tone is what makes something sound like it is being
#     shaped by a living thing rather than played.
#
#   JITTER. Nothing alive holds a pitch. A slow random walk of a few per cent
#     on the fundamental is the single cheapest thing that stops a sound
#     reading as a machine.
#
#   TAILS. Long dense reverb, so it arrives from somewhere with a size.


def buf(seconds):
	return [0.0] * int(RATE * seconds)


def _walk(n, depth, seed, speed=0.6):
	"""A slow random drift, one value per sample. Nothing alive holds a pitch."""
	rng = random.Random(seed)
	out = [0.0] * n
	value = 0.0
	target = 0.0
	step = max(1, int(RATE / speed / 40))
	for i in range(n):
		if i % step == 0:
			target = (rng.random() * 2.0 - 1.0) * depth
		value += (target - value) * 0.0008
		out[i] = value
	return out


def fm(out, carrier, ratio, index, gain=1.0, seed=0,
       carrier_to=None, index_to=None, drift=0.0):
	"""Frequency modulation. The single most important function in this file.

	`ratio` is the modulator's frequency as a multiple of the carrier — whole
	numbers give harmonic, musical results and fractions give the inharmonic,
	bell-and-throat timbres this mod wants. `index` is how hard it is driven,
	which is essentially how bright and how metallic.
	"""
	n = len(out)
	wobble = _walk(n, drift, seed) if drift else None
	cp = 0.0
	mp = 0.0
	for i in range(n):
		t = i / n
		f = carrier if carrier_to is None else carrier + (carrier_to - carrier) * t
		if wobble:
			f *= 1.0 + wobble[i]
		k = index if index_to is None else index + (index_to - index) * t
		mp += 2.0 * math.pi * (f * ratio) / RATE
		cp += 2.0 * math.pi * f / RATE + math.sin(mp) * k * 0.01
		out[i] += math.sin(cp) * gain
	return out


def sine(out, freq, gain=1.0, phase=0.0, sweep=None):
	n = len(out)
	p = phase
	for i in range(n):
		f = freq if sweep is None else freq + (sweep - freq) * (i / n)
		p += 2.0 * math.pi * f / RATE
		out[i] += math.sin(p) * gain
	return out


def noise(out, gain=1.0, seed=0):
	rng = random.Random(seed)
	for i in range(len(out)):
		out[i] += (rng.random() * 2.0 - 1.0) * gain
	return out


def resonant(out, cutoff, q=4.0, cutoff_to=None):
	"""Two-pole state-variable lowpass, and it RINGS.

	The one-pole filter the first pass used only removes; this one resonates at
	its cutoff, which is what gives a sound a body instead of a shape. Sweeping
	the cutoff is a mouth opening.
	"""
	n = len(out)
	low = 0.0
	band = 0.0
	for i in range(n):
		fc = cutoff if cutoff_to is None else cutoff + (cutoff_to - cutoff) * (i / n)
		f = 2.0 * math.sin(math.pi * min(fc, RATE * 0.45) / RATE)
		damp = 1.0 / max(0.5, q)
		high = out[i] - low - damp * band
		band += f * high
		low += f * band
		out[i] = low
	return out


def lowpass(out, cutoff):
	a = 1.0 - math.exp(-2.0 * math.pi * cutoff / RATE)
	last = 0.0
	for i in range(len(out)):
		last += a * (out[i] - last)
		out[i] = last
	return out


def highpass(out, cutoff):
	a = math.exp(-2.0 * math.pi * cutoff / RATE)
	last_in = 0.0
	last_out = 0.0
	for i in range(len(out)):
		cur = out[i]
		last_out = a * (last_out + cur - last_in)
		last_in = cur
		out[i] = last_out
	return out


def shape(out, points):
	n = len(out)
	for i in range(n):
		t = i / n
		gain = points[-1][1]
		for j in range(len(points) - 1):
			a, b = points[j], points[j + 1]
			if a[0] <= t <= b[0]:
				span = b[0] - a[0]
				k = 0.0 if span == 0 else (t - a[0]) / span
				gain = a[1] + (b[1] - a[1]) * k
				break
		out[i] *= gain
	return out


def mix(into, other, gain=1.0):
	for i in range(min(len(into), len(other))):
		into[i] += other[i] * gain
	return into


def soften(out, amount=0.7):
	for i in range(len(out)):
		out[i] = math.tanh(out[i] * amount) / math.tanh(amount)
	return out


def reverb(out, size=0.6, wet=0.35):
	"""Comb filters into allpasses — a Schroeder reverb, near enough.

	The first pass used two delay taps, which is an echo rather than a space.
	Four combs at prime-ish lengths with feedback give a tail that has no
	audible repeats in it, and that is what makes a sound arrive from somewhere
	with a size instead of from a wall.
	"""
	n = len(out)
	src = list(out)
	tail = [0.0] * n
	for ms, fb in ((37.1, 0.80), (41.7, 0.78), (49.3, 0.76), (57.9, 0.74)):
		d = int(RATE * ms * size / 1000.0)
		line = [0.0] * max(1, d)
		idx = 0
		for i in range(n):
			delayed = line[idx]
			line[idx] = src[i] + delayed * fb
			idx = (idx + 1) % len(line)
			tail[i] += delayed * 0.25
	for ms, g in ((5.3, 0.7), (1.7, 0.7)):
		d = int(RATE * ms * size / 1000.0)
		line = [0.0] * max(1, d)
		idx = 0
		for i in range(n):
			delayed = line[idx]
			value = tail[i] + delayed * -g
			line[idx] = value
			idx = (idx + 1) % len(line)
			tail[i] = delayed + value * g
	for i in range(n):
		out[i] = out[i] * (1.0 - wet) + tail[i] * wet
	return out


def seamless(out, fade_seconds):
	f = int(RATE * fade_seconds)
	n = len(out)
	head = out[:f]
	for i in range(f):
		k = i / f
		out[n - f + i] = out[n - f + i] * (1.0 - k) + head[i] * k
	return out[f:]


def normalise(out, peak=0.85):
	high = max(abs(v) for v in out) or 1.0
	scale = peak / high
	return [v * scale for v in out]


def write(name, samples, stream=False):
	os.makedirs(OUT, exist_ok=True)
	wav = os.path.join(OUT, name + '.wav')
	ogg = os.path.join(OUT, name + '.ogg')
	with wave.open(wav, 'wb') as f:
		f.setnchannels(1)
		f.setsampwidth(2)
		f.setframerate(RATE)
		f.writeframes(b''.join(
			struct.pack('<h', int(max(-1.0, min(1.0, v)) * 32000)) for v in samples))
	subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', wav,
	                '-c:a', 'libvorbis', '-q:a', '2', ogg], check=True)
	os.remove(wav)
	print('  %-14s %5.1fs  %6.1f KB%s'
	      % (name, len(samples) / RATE, os.path.getsize(ogg) / 1024,
	         '  (streamed)' if stream else ''))


# ---- the sounds --------------------------------------------------------
def breath():
	"""BEHIND THE ROCK, AND IT IS NOT A HEARTBEAT.

	Rebuilt much darker. The first pass was a sine thump and filtered noise,
	which is a drum and some air — clean, tidy and completely inert. This is FM
	at a fractional ratio, so the thump has inharmonic partials under it and
	reads as a body rather than a beater, and the exhale is driven through a
	resonant filter that opens and closes like a throat.

	The fundamental drifts, because nothing alive holds a pitch, and the whole
	thing sits in a long tail so it arrives through stone rather than from a
	speaker.
	"""
	out = buf(4.0)
	for at, gain, tune in ((0.05, 1.0, 1.0), (0.33, 0.7, 0.94)):
		thump = buf(0.9)
		# Ratio 1.41 — deliberately irrational-ish, so no partial lands on a
		# harmonic and the result has no note in it.
		fm(thump, 44.0 * tune, 1.41, 6.0, 1.0, seed=int(at * 100),
		   carrier_to=31.0 * tune, index_to=1.2, drift=0.02)
		resonant(thump, 220.0, 3.0, cutoff_to=90.0)
		shape(thump, [(0, 0), (0.03, 1), (0.30, 0.3), (1, 0)])
		start = int(RATE * at)
		for i in range(len(thump)):
			if start + i < len(out):
				out[start + i] += thump[i] * gain
	air = buf(4.0)
	noise(air, 0.6, seed=11)
	resonant(air, 180.0, 6.0, cutoff_to=520.0)
	highpass(air, 60.0)
	shape(air, [(0, 0), (0.30, 0.06), (0.55, 0.6), (0.75, 0.3), (1, 0)])
	mix(out, air, 0.9)
	reverb(out, 0.85, 0.42)
	return normalise(out, 0.6)


def anger():
	"""HE HAS BEEN HURT, AND THE ROOM GETS HEAVIER.

	Still not a roar — but far more teeth than the first pass, which was a clean
	sub-bass slide and sounded like a synthesiser doing a sad noise.

	The character comes from driving the FM index HARD at the start and letting
	it collapse: high index is a spray of inharmonic sidebands, which is metal
	and grit and something tearing, and as it falls away what is left underneath
	is just the low fundamental. So it opens as a sound you cannot identify and
	resolves into pressure. That shape — chaos settling into weight — is what
	makes it read as something losing its temper rather than something roaring.
	"""
	out = buf(2.6)
	fm(out, 88.0, 2.73, 34.0, 0.9, seed=5,
	   carrier_to=33.0, index_to=1.5, drift=0.035)
	fm(out, 131.0, 1.49, 18.0, 0.3, seed=9, carrier_to=49.0, index_to=0.8)
	resonant(out, 900.0, 5.0, cutoff_to=140.0)
	grit = buf(2.6)
	noise(grit, 1.0, seed=23)
	resonant(grit, 1400.0, 8.0, cutoff_to=200.0)
	shape(grit, [(0, 0), (0.08, 0.8), (0.4, 0.3), (1, 0)])
	mix(out, grit, 0.45)
	sub = buf(2.6)
	sine(sub, 41.0, 1.0, sweep=27.0)
	shape(sub, [(0, 0), (0.06, 1), (0.6, 0.7), (1, 0)])
	mix(out, sub, 0.55)
	shape(out, [(0, 0), (0.03, 1), (0.5, 0.6), (1, 0)])
	soften(out, 2.2)
	# A BIG ROOM, because the returns need something to be a return OF.
	#
	# ModSounds.roll now throws this sound back off the country a beat later, and
	# that only reads as a landscape if the direct hit already sounds like it is
	# outdoors. A tight, dry hit followed by distant repeats sounds like two
	# different sounds; a wet one followed by them sounds like one sound in a
	# valley. Bigger and wetter than anything else here for that reason alone.
	reverb(out, 0.95, 0.44)
	return normalise(out, 0.85)


def gone():
	"""HIS DEATH, AND IT IS A RELEASE RATHER THAN A SCREAM.

	The longest thing here and the only one with somewhere to go. A slow FM
	collapse — carrier falling, index falling with it — under a resonant sweep
	that closes from wide open down to nothing, so the sound narrows as it
	descends and finally has no top left at all.

	The tail is the point. It goes on well past the last of the tone, and what
	the player is listening to for the final second and a half is a room with
	nothing in it.
	"""
	out = buf(5.5)
	fm(out, 116.0, 1.97, 14.0, 0.75, seed=17,
	   carrier_to=24.0, index_to=0.4, drift=0.02)
	fm(out, 74.0, 3.51, 7.0, 0.35, seed=29, carrier_to=19.0, index_to=0.2)
	resonant(out, 1800.0, 4.0, cutoff_to=70.0)
	wash = buf(5.5)
	noise(wash, 1.0, seed=41)
	resonant(wash, 900.0, 3.0, cutoff_to=120.0)
	shape(wash, [(0, 0.5), (0.25, 0.3), (0.7, 0.08), (1, 0)])
	mix(out, wash, 0.4)
	shape(out, [(0, 0), (0.02, 1), (0.35, 0.55), (0.8, 0.12), (1, 0)])
	reverb(out, 1.0, 0.5)
	return normalise(out, 0.72)


def his_world():
	"""THE BED FOR HIS WORLD, AND IT HAS TO LOOP FOREVER.

	Darker and less tonal than the first pass, which was three sines and read as
	a hum. The body of it is now a very low FM pair at a fractional ratio, so
	what sits under the dimension has partials that do not belong to any note —
	present, unplaceable, and impossible to hum back.

	Three things move on different periods and none of them divide into each
	other: the beat between the two drones, the wind, and a resonant sweep that
	takes fifteen seconds to open and close. Twenty-two seconds of loop with
	three incommensurate cycles in it never repeats the same combination twice
	within a session, which is what stops a bed becoming furniture.
	"""
	length = 24.0
	out = buf(length)
	fm(out, 41.0, 1.37, 3.2, 0.5, seed=3, drift=0.012)
	fm(out, 41.55, 1.37, 2.8, 0.45, seed=4, drift=0.012)
	sine(out, 27.5, 0.35)
	wind = buf(length)
	noise(wind, 1.0, seed=7)
	resonant(wind, 300.0, 5.0)
	for i in range(len(wind)):
		t = i / RATE
		wind[i] *= 0.45 + 0.55 * math.sin(2.0 * math.pi * t / 9.4)
	mix(out, wind, 0.4)
	# A resonance that opens and closes on its own slow period, so the colour of
	# the bed moves even where nothing about the notes does.
	sweep = buf(length)
	noise(sweep, 1.0, seed=52)
	resonant(sweep, 140.0, 9.0, cutoff_to=620.0)
	shape(sweep, [(0, 0.1), (0.4, 0.35), (0.75, 0.12), (1, 0.1)])
	mix(out, sweep, 0.18)
	settle = buf(length)
	noise(settle, 1.0, seed=63)
	resonant(settle, 800.0, 7.0)
	highpass(settle, 240.0)
	shape(settle, [(0, 0), (0.58, 0), (0.60, 0.45), (0.68, 0), (1, 0)])
	mix(out, settle, 0.2)
	reverb(out, 1.0, 0.45)
	return normalise(seamless(out, 2.0), 0.5)


def the_way():
	"""THE FRAME CLOSING OVER HIM.

	The one sound here that is allowed to be clean. White is his colour and the
	door is white light behind black, so this stays thin and bright where
	everything else is buried — but it is given a resonant sweep upward and a
	very low anchor underneath, so it is not merely pretty. Something finishing
	rather than something opening.
	"""
	out = buf(3.4)
	fm(out, 196.0, 2.01, 5.0, 0.4, seed=13, carrier_to=466.0, index_to=1.0)
	sine(out, 294.0, 0.16, sweep=699.0)
	sine(out, 47.0, 0.55, sweep=39.0)
	resonant(out, 400.0, 6.0, cutoff_to=3400.0)
	shine = buf(3.4)
	noise(shine, 1.0, seed=88)
	highpass(shine, 2600.0)
	shape(shine, [(0, 0), (0.3, 0.45), (0.6, 0.18), (1, 0)])
	mix(out, shine, 0.28)
	shape(out, [(0, 0), (0.08, 0.9), (0.4, 1), (1, 0)])
	reverb(out, 0.9, 0.4)
	return normalise(out, 0.72)


def crossing():
	"""GOING THROUGH IT, WHICH IS NOT THE SAME AS OPENING IT.

	`the_way` is the frame lighting — one event, once, at the end of a fight.
	This is the two and a half seconds a person spends inside it, and it was
	vanilla's nether travel sound: the single most familiar audio cue in the game
	attached to the one door in the mod that is supposed to be unprecedented.

	A PRESSURE CHANGE RATHER THAN A WHOOSH. The low FM layer rises through the
	first half and then falls further than it started, so the sound arrives
	somewhere lower than it left — the ear reads that as descent regardless of
	which way the player is actually travelling. Over it, a noise wash whose
	filter opens wide and then shuts almost completely, which is the part that
	feels like a room being replaced by a different room.

	The reverb is the largest in the file and it is deliberately still ringing
	when the tone has gone, because the last half second should be the new place
	rather than the crossing. Long enough to cover the chunk load, which is a
	practical concern and also the honest reason it works: nobody has ever heard
	silence on the far side of this.

	Pitched at the call site rather than here — down going out, up coming home,
	so the same asset carries the direction. See TheWayBlock.
	"""
	out = buf(2.6)
	fm(out, 88.0, 1.51, 12.0, 0.7, seed=131, carrier_to=290.0, index_to=2.0,
	   drift=0.03)
	fall = buf(2.6)
	fm(fall, 260.0, 2.49, 9.0, 0.55, seed=149, carrier_to=33.0, index_to=0.2)
	shape(fall, [(0, 0), (0.45, 0.2), (0.6, 1.0), (1, 0.15)])
	mix(out, fall, 0.8)
	wash = buf(2.6)
	noise(wash, 1.0, seed=163)
	resonant(wash, 260.0, 3.2, cutoff_to=4200.0)
	shape(wash, [(0, 0.1), (0.4, 0.9), (0.62, 0.5), (1, 0.02)])
	mix(out, wash, 0.34)
	resonant(out, 3000.0, 2.4, cutoff_to=190.0)
	shape(out, [(0, 0), (0.06, 0.9), (0.5, 1.0), (0.72, 0.4), (1, 0)])
	reverb(out, 1.0, 0.52)
	return normalise(out, 0.8)


def hum():
	"""THE VILLAGER NOISE, WITH SOMETHING WRONG IN THE THROAT.

	Vanilla's "hmm" is a closed-mouth hum, about two tenths of a second, mid-range
	and almost cheerful — and it is the single most recognisable friendly sound in
	the game. Which is exactly why it is worth ruining: a crowd of villagers is not
	frightening, and a crowd of villagers making a sound that is ALMOST that is.

	FOUR THINGS DONE TO IT, and each one is small on purpose. Drop the fundamental
	from speech into chest range. Nasal formants only — a closed mouth, so it is
	still a hum rather than a groan. A break in the middle where the note fails and
	comes back a semitone under, which is what a hum does when the throat producing
	it is damaged. And a wet edge of noise under the whole thing.
	
	NOT A MONSTER SOUND. Nothing here growls, screams or rasps; the moment it does
	the player files it under "hostile mob" and stops listening. It has to stay
	close enough to the original that the first one is ambiguous — did that sound
	off, or am I imagining it — and only obviously wrong once there are twelve.

	Longer than vanilla's, at half a second, because the failure in the middle needs
	somewhere to happen.
	"""
	out = buf(0.55)

	voice = buf(0.55)
	# Two thirds the pitch of a villager, wavering, with the break at 45%.
	fm(voice, 118.0, 1.0, 2.4, 0.9, seed=307, carrier_to=104.0, index_to=1.4,
	   drift=0.05)
	under = buf(0.55)
	fm(under, 111.0, 1.0, 2.0, 0.6, seed=311, carrier_to=98.0, index_to=1.1,
	   drift=0.06)
	shape(under, [(0, 0), (0.42, 0), (0.5, 1.0), (1, 0.9)])
	shape(voice, [(0, 1.0), (0.42, 1.0), (0.5, 0.15), (0.62, 0.5), (1, 0.4)])
	mix(voice, under, 0.85)

	# The wet edge. Very little of it — this is a throat, not a rattle.
	spit = buf(0.55)
	noise(spit, 1.0, seed=313)
	lowpass(spit, 1100.0)
	shape(spit, [(0, 0.6), (0.45, 0.2), (0.55, 1.0), (1, 0)])
	mix(voice, spit, 0.13)

	# NASAL: one low formant and one at two and a half, nothing above. A mouth
	# that never opens.
	for freq, gain in ((250.0, 1.0), (620.0, 0.5)):
		band = list(voice)
		resonant(band, freq, 7.0)
		if freq > 400.0:
			highpass(band, 300.0)
		mix(out, band, gain)

	shape(out, [(0, 0), (0.05, 1.0), (0.85, 0.7), (1, 0)])
	lowpass(out, 1600.0)
	reverb(out, 0.45, 0.2)
	return normalise(out, 0.7)


SOUNDS = {
	'breath': (breath, False),
	'anger': (anger, False),
	'gone': (gone, False),
	'his_world': (his_world, True),
	'the_way': (the_way, False),
	'crossing': (crossing, False),
	'hum': (hum, False),
}


def main():
	if subprocess.run(['ffmpeg', '-version'], capture_output=True).returncode:
		raise SystemExit('ffmpeg is needed to write Ogg Vorbis')
	print('synthesising %d sounds' % len(SOUNDS))
	for name, (make, stream) in SOUNDS.items():
		write(name, make(), stream)


if __name__ == '__main__':
	main()
