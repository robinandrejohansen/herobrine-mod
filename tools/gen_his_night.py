#!/usr/bin/env python3
"""A timeline that is midnight and stays midnight.

26.2 moved time out of a number on the level and into CLOCKS and TIMELINES: a
clock counts ticks, and a timeline is a set of keyframed tracks — sky colour,
sun angle, star brightness, how much light the sky gives — read off that clock.
The overworld's day/night cycle is `minecraft:day`, nine kilobytes of keyframes
over a 24000-tick period.

WHICH IS WHY THIS IS GENERATED RATHER THAN HAND-WRITTEN. There are eighteen
tracks and their value types are all different — booleans, floats, packed colour
strings, angles — and every one of them would have to be guessed at correctly for
a hand-authored file to produce a sky rather than a crash. Sampling vanilla's own
file at midnight and emitting the answer cannot get any of them wrong.

So: read `minecraft:day`, take the value each track holds at tick 18000, and
write those out as SINGLE keyframes. A track with one keyframe never changes, so
the clock can tick as fast as it likes and the sky stays exactly where midnight
put it. No clock has to be paused and no Java has to touch time at all.

It is bound to a clock of our own — `herobrine:his_night` — for the one reason
that matters on a live server: `/time set day` in the overworld must not do
anything to his world, and sleeping through a night at home must not either.

Run:  python3 tools/gen_his_night.py
"""
import glob
import json
import os
import sys
import zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
DATA = os.path.join(HERE, '..', 'src', 'main', 'resources', 'data', 'herobrine')

CLOCK = 'herobrine:his_night'
MIDNIGHT = 18000

# Tracks worth overriding after sampling, and why each one.
OVERRIDE = {
	# Pitch dark from the sky, so every hostile spawns everywhere and a torch is
	# the only thing between the player and the wood. Midnight is already 0 in
	# vanilla; stated here so it cannot drift if Mojang retunes the curve.
	'minecraft:gameplay/sky_light_level': 0,
	# Nothing burns at dawn, because there is no dawn. Sampling midnight would
	# give this anyway; pinned because the whole dimension depends on it.
	'minecraft:gameplay/monsters_burn': False,
	# The creaking is awake. It is vanilla's own dark-forest horror and it is
	# normally only active at night — in a place that is only ever night, it is
	# simply a resident.
	'minecraft:gameplay/creaking_active': True,
}


def client_jar():
	jars = glob.glob(os.path.expanduser(
		'~/.gradle/caches/fabric-loom/**/minecraft-client.jar'), recursive=True)
	if not jars:
		raise SystemExit('no client jar; run ./gradlew genSources first')
	return zipfile.ZipFile(jars[0])


def at(track, ticks):
	"""The value a keyframed track holds at a given tick.

	Last keyframe at or before the time, wrapping to the final frame of the
	period if the time falls before the first — which is how a cyclic track
	behaves and is what midnight needs, since several of these start after it.
	"""
	frames = track.get('keyframes')
	if not frames:
		return track
	best = frames[-1]
	for frame in frames:
		if frame['ticks'] <= ticks:
			best = frame
	return best['value']


def main():
	day = json.loads(client_jar().read('data/minecraft/timeline/day.json'))

	tracks = {}
	for name, track in day['tracks'].items():
		value = OVERRIDE[name] if name in OVERRIDE else at(track, MIDNIGHT)
		one = {'keyframes': [{'ticks': 0, 'value': value}]}
		# Carry the modifier through — a track that vanilla combines with `and`
		# behaves differently from one that replaces, and dropping it silently
		# changes what the value means.
		if 'modifier' in track:
			one['modifier'] = track['modifier']
		tracks[name] = one

	# No time_markers. Those are the keys /time and the villager schedule move
	# to, they are registered per marker rather than per timeline, and a second
	# file claiming minecraft:midnight would be fighting the overworld for them.
	timeline = {
		'clock': CLOCK,
		'period_ticks': day['period_ticks'],
		'tracks': tracks,
	}

	write(os.path.join(DATA, 'world_clock', 'his_night.json'), {})
	write(os.path.join(DATA, 'timeline', 'his_night.json'), timeline)
	write(os.path.join(DATA, 'tags', 'timeline', 'in_his_world.json'),
	      {'values': ['herobrine:his_night']})
	print('%d tracks pinned at midnight' % len(tracks))
	for name in sorted(OVERRIDE):
		print('  overridden  %s = %s' % (name, OVERRIDE[name]))


def write(path, body):
	os.makedirs(os.path.dirname(path), exist_ok=True)
	with open(path, 'w') as f:
		json.dump(body, f, indent=2, sort_keys=True)
		f.write('\n')
	print('  wrote %s' % os.path.relpath(path, os.path.join(HERE, '..')))


if __name__ == '__main__':
	main()
