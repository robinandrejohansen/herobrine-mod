# -*- coding: utf-8 -*-
"""Every book in the mod. Prose in, paginated Java out.

TEN BOOKS, NUMBERED, IN ORDER, ONE VOICE.

The old set was twenty-two books in ten hands. Alma's house book, Toby's book,
Joren's protocol, Kadmus on the watch, and Steve's six documents laid over the
top of them. Every one of them was good and the whole was unreadable: a player
finds these one at a time, hours apart, between two other things, and asking
them to assemble a plot out of ten strangers' diaries is asking for something
nobody does.

So it is one man now and the books are numbered 1 to 10. He is called Addexio,
he lived in the first house, and he is the companion you can still find alive
under the town — which is the whole reason the numbering matters. Book 10 is
addressed to you and ends by telling you where he is.

HE IS A HERO WHO FAILED. Not a chronicler and not a victim. He was there for all
of it, he had a weapon every time, and the story he is telling is the story of
what he did not do. That is what makes ten books in one voice worth reading
instead of exhausting: the arc is his, and it goes downward.

WHAT EACH ONE HAS TO DO. Three jobs, every time, or it is scenery:

  MAKE THIS PLACE MATTER   what happened in the room you are standing in
  POINT AT THE NEXT ONE    by name, with a reason to walk there
  MOVE THE STORY ON        so the order is not decoration

EASY WORDS AND SHORT LINES. A book page is about nineteen characters wide and
fourteen rows tall, so a long word costs a line and a subordinate clause costs a
page. It is also read on a screen by somebody who is being hunted. Short
sentences, plain words, one idea a paragraph.

PAGINATION IS NOT OPTIONAL. Overflow is SILENT — a row past fourteen is simply
not drawn, and seven pages of the first set were clipped for months without
anybody noticing. bind() does the layout and gen_books.py asserts every page.
Titles are capped at 32 characters because a longer one throws an
EncoderException and disconnects the player.
"""

ADDEXIO = "Addexio"
# Steve still writes his own lab set, below. He is a character in Addexio's
# account now rather than its narrator — see book 7.
STEVE = "Steve"

one = ("1. the farm", ADDEXIO, """
1. THE FARM.

My name is Addexio. I lived here.

I am leaving ten of these. One at every place it happened. Read them in order and you will know what I know.

There is a map in this chest. I drew it. It shows the next place.

I grew wheat in the north field for eleven years. I had a wife. We had no children, and I was glad of that later.

WHO WAS HERE BEFORE ME.

A man called Steve farmed this valley first. He had a friend. The two of them went under the hill behind my north field looking for iron, and they found a door down there.

Steve did not touch it.

His friend did.

His friend is called Herobrine. Write the name down. Everyone here learned to say "him" instead, and saying "him" is how a thing grows.

WHAT I SAW FIRST.

Small things. My torches went out in a line, one after another, from the barn to the house.

A door I had shut standing open. Not swinging. Open, and still.

Boot prints in the wet field going one way only. Out, and never back.

I told myself it was foxes. I told my wife it was foxes. She stopped asking after a while, and that was worse.

WHERE TO GO.

Under my outbuilding there is a passage. I dug it myself. The next book is in a crate at the end of it.

Go down there first. It says why I dug it.
""")

two = ("2. what I saw", ADDEXIO, """
2. WHAT I SAW.

I dug this passage to hide my wife in.

That is the truth and it took me a year to write it down.

Eleven feet down and thirty long, at night, in the spring, so that nobody would ask. I told the neighbours it was for roots.

WHAT MADE ME DIG.

I was walking the fence line at dusk.

There was a man standing in my wheat, forty paces off. Grey shirt. Arms down. Facing me.

I raised my hand to him.

He did not move. I counted to a hundred and he did not move, and he did not breathe, and the wheat did not move around him either.

Then I looked at his face. There was nothing in it. Just white, where eyes go.

I ran. I am not ashamed of the running.

WHAT I DID NEXT.

I went to my neighbours. Otto, and Marek, and old Bren who could not walk far.

I helped them board their windows. I carried Marek's children to the town myself, two at a time, because he had four and one horse.

And I told all of them it would pass.

I was wrong. I have been wrong about nearly every single thing in these ten books. Read them anyway. Being wrong in order is still a map.

WHERE TO GO.

On the eleventh day a rider came down the valley road with blood on his coat and could not finish a sentence.

Ashfold. The walled town, north of here. It had already started there.

The map from the farmhouse points at it. Go.
""")

three = ("3. the town", ADDEXIO, """
3. THE TOWN.

Ashfold had four hundred people and a wall, and the wall is why nobody was afraid.

I got here on the twelfth day. Not the first day. The twelfth. I want that written down.

WHAT WAS ALREADY WRONG.

The animals. Every dog in the town was facing the same way and not one of them was barking.

The bell in the church rang once, at nothing. I climbed up to see who had pulled it. There was nobody in the tower.

And the people in the street were talking about ordinary things, and it took me an hour to work out why that felt so bad.

They were all saying the same ordinary things.

THE ONES WEARING FACES.

A woman I knew called my name from a doorway.

She had my name right and her mouth was wrong. It moved a moment after the sound came out.

I said her husband's name back to her. She smiled and did not answer. She did not know it.

There were nine of them in the town by then. Nine people who were not those people any more. You could stand a foot away and still not be sure.

THE TALL ONE.

Then something came down the main street that was not pretending to be anybody.

Three of us watched it from the smithy roof. It had the shape of a villager and it was as tall as the doorframes, and its mouth was open, and it stayed open.

It did not run. It walked. It did not stop walking, and everything in front of it moved aside or stopped being able to.

Bren shot it twice. It turned its head, looked at him, and kept walking.

WHERE TO GO.

The next book is in this same chest.

I wrote it four years later in a cellar and I could not finish it in one sitting. Read it anyway.

You need to know what he actually came here for.
""")

four = ("4. what he did here", ADDEXIO, """
4. WHAT HE DID HERE.

I have started this book nine times.

WHAT HE CAME FOR.

He did not come to kill the town. Killing the town was tidying up afterwards.

He came for the ones who would fit through the door.

THE DOOR.

On the third night there was a door standing in the square with nothing around it.

Not a doorway. A frame of black stone, and inside it a purple light that moved like water standing on its end.

They walked people through it. The nine wearing faces took them by the arms, and the tall one came behind, and the people went, because the things holding them had their sisters' faces on.

Forty-one people went through. I counted the ones who never came out and forty-one is the number.

I do not know where it goes. The air that came out of it was cold and smelled like a struck match. Nobody has ever come back through it.

WHAT HE DID TO THE REST.

The rest he did himself, in the open, in the middle of the day, so that we would watch.

He started with the ones who could not run.

Marek's four children were in the church with the cleric and eleven women. He came in through the roof. He did not use a sword. There was almost no noise and it took him less than a minute.

I was thirty feet away, behind a cart, with an iron sword in my hand.

I did not move.

That is the sentence. I have written nine versions of this book to get around it. I had a weapon, I was close enough, and I stayed behind the cart.

When he came out he looked straight at me. He knew exactly where I was.

He let me live. I have thought about why for sixty years. I think somebody had to tell it.

AND THEN HE WAS GONE.

Two hundred and six dead. Forty-one through the door. He walked out of the north gate at dawn and the valley did not see him again for nine years.

Some of what we buried we could not tell apart, so we buried it together and put up one stone.

WHERE TO GO.

Nine years is a long time to feel safe and we used it badly.

We built a tower north of here to watch for him. It is still standing. The next book is at the top of it.

Watching was not the problem.
""")

five = ("5. the tower", ADDEXIO, """
5. THE TOWER.

We built this to watch the horizon.

Three of us put it up in one summer. Otto, a lad called Pip who was seventeen, and me. Ninety feet of stone, a room at the top, and a bell.

Somebody stood in it every night for nine years. I took the winter watches. I do not sleep well anyway.

WHAT WAS WRONG WITH IT.

Everything. It is a good tower and it is the most stupid thing I have ever helped build.

We watched the horizon because we thought he would come from somewhere.

He does not come from somewhere. He is already where you are, and the reason you cannot see him is not distance.

WHAT PIP FOUND.

In the ninth summer Pip went down to the spring below the tower to clear the pipe.

He came back up the ladder shaking so hard he could not hold the rungs.

He said there was a hole in the rock that had not been there yesterday, with steps going down it, cut square.

He said there was a light at the bottom the colour of a bruise.

He said he could hear somebody counting.

We did not believe him. Write that down too.

Four of us went down the next morning to prove there was nothing.

Two of us came back up.

WHERE TO GO.

Here is the part nobody is ready for, and it is worse than the other way round.

What was under this tower was not his.

It was ours. Men from the town cut those steps, and they had been cutting them the whole nine years we spent up here looking at the sky.

The map points at it. It is a prison. Fourteen cells, iron on every door, and a desk at the end with a ledger on it.

Read the ledger. Then read the book beside it.
""")

six = ("6. the prison", ADDEXIO, """
6. THE PRISON.

Fourteen cells cut into rock. Iron on every door. A desk at the far end with a lamp.

The men who dug this were not monsters. That goes at the top, because it is the only part that is actually frightening.

WHAT IT WAS FOR.

Some people came back.

Not through the door in the square. From smaller things. A night lost in the woods. A shaft that went too deep. A face at a window. They would be gone a day or a week, and then they would walk home.

And they would be almost right.

They knew their own names. They knew their children. They would sit at their own table and eat with their own hands, and then one evening they would say a sentence that only a dead man could know.

So the town built somewhere to put them.

HOW IT WORKED.

You brought them in and you counted them in. It is painted on the wall by the gate. COUNT THEM IN. COUNT THEM OUT.

The warder was a fair man called Joren and he kept a careful ledger, and that ledger is the worst object in this valley.

It has two columns. In, and out.

The out column is shorter. The missing names are not marked dead.

HOW TO TELL, AFTER ALL THAT.

They did work it out in the end. Not from the cutting. From watching. Learn this properly, because you will need it and I will not be there.

It comes to you. A person waits to be spoken to. It closes the last six paces itself.

It copies you. Crouch, and it crouches. Do it twice and you will see the delay.

It goes through your things. Chests, barrels, anything with a lid. It never takes anything.

It changes its coat. Look away for a minute and the colour is not the same.

And it only ever does any of this when you are on your own. Stay in sight of somebody else and it will stand there being ordinary for as long as you like.

WHERE TO GO.

There is a second book in this chest and it is not mine.

It is what one man said out loud, in a room with me and two others, over about four hours, after we got him out of cell nine.

I wrote it down as he said it. I did not tidy it.

If you would rather not, close the chest. It does not change what you have to do.
""")

seven = ("7. the one who came back", ADDEXIO, """
7. THE ONE WHO CAME BACK.

Taken down as spoken. Cell nine. Eleven weeks inside.

I have not tidied his words. Where he stopped, I have left the gap.

"They put me in on a Tuesday. Joren wrote my name in the book. He said sorry. He did say sorry."

"The first week was only questions. What is your wife called. What did you eat. Say the alphabet backwards. Easy things. I got every one of them right."

"Then a man came who was not from the town."

"He had a book and he asked the same questions and he did not listen to the answers. He was watching my hands. He said the answers do not matter. Only the hands."

"They took the door off cell four and put a lamp in and left it burning nine days, and the man in there never slept, and on the ninth day he was still not wrong. So they wrote down that the lamp does not work."

"They tried cold. They tried keeping us awake. Then they tried cutting, to see if we healed the way a man heals."

"I healed the way a man heals. So they cut deeper, because the shallow ones only proved the shallow ones."

"There was a woman in six. I will not give her name. Her daughter is still alive."

"They opened her arm from the elbow down and held it open with pins and asked her to name her mother, and she did, all the way through, and she was right every time."

"She was still right when she died."

"Here is what you have to understand. She was not one of them. Nobody in this hole was one of them."

"We were the ordinary ones. That is the point of us. You cannot tell them apart, so you have to do it to the ordinary ones as well, or the numbers mean nothing."

"He wrote that in his book. He said it just like that. The numbers mean nothing."

"Eleven of us went in. I came out."

WHO THE MAN WITH THE BOOK WAS.

He was Steve.

The same Steve who farmed my valley before me. The same Steve who went under the hill and did not touch the door.

Herobrine's friend.

He was not trying to save anybody. He was trying to find out whether he himself would have come up out of that hole the same way.

He found out that he would.

WHERE TO GO.

We closed the prison. We hanged nobody. I think about that.

Then in the eleventh year two boys found where he lives.

The church is south of here. Everyone who was left went there first, and there is a book on the altar.
""")

eight = ("8. where he lives", ADDEXIO, """
8. WHERE HE LIVES.

He does not live in a house.

Two boys following a dog found it in the eleventh year. A frame of black stone standing in a hollow, with the same purple light standing up inside it like water.

The same door as the one in the square at Ashfold. He had one out here the whole time.

WHAT THIS CHURCH WAS FOR.

Everything left of us came here first.

Not to pray, although we did that too. To decide. Two hundred people in one room, deciding whether to walk into a door.

The cleric here was called Wendel. He was old, he was not brave, and he was the only one of us who said the true thing out loud.

He said: before you let that man near you, ask Steve what he did to Herobrine under the hill.

We told him it was not the time.

So he wrote it down instead, which is why you can still read it.

He was dead inside the month. Steve was four days' ride away, knew which night it was, and did not come.

WHAT IS THROUGH IT.

I went through. Nine of us went and four came back. I am one of the four and I will not write down most of it.

I will write down this much, because you will need it.

It is not a cave. Somebody built it.

There is a castle over there with lights in the windows, and a road going up to it, and the road is paved, and the paving is laid in a pattern.

He did not find a hole and crawl into it. He went somewhere and built a house.

WHERE TO GO.

Follow the map west. There is a small village at the end of it with a hall in the middle and a flag on the hall.

That is where it stopped. That is where I nearly stopped with it.
""")

nine = ("9. the last house", ADDEXIO, """
9. THE LAST HOUSE.

Sixty people lived in this village.

We came here in the twelfth year with everything we had left, because we had finally worked out how to make him come to us.

You make him come by taking his door.

HOW IT WENT.

We drove him out of Ashfold first. Two hundred of us with iron and fire, in the streets where we had buried our own families, and it worked, and I have never been so certain of anything in my life.

Then out of the fields. Then along the valley road for two days, losing four and five and six of us a night, and every one of them was somebody I had eaten with.

Then here. To the hall with the flag on it, because his last door was underneath it.

WHAT HAPPENED IN THE HALL.

He was waiting in the hall.

He had let us drive him. Twelve days of us thinking we were pushing, and he had been walking backwards to the one place where all of us would be standing in one room.

I got to him. That is the only thing in ten books I am not ashamed of. I got close enough to swing, and I swung, and I hit him, and it was like hitting a doorframe.

Then I was against the far wall with a great deal of my own blood on the boards and my left hand would not work.

It has never worked properly since.

Sixty-one of us died in this village. In one room. In about the time it takes to boil a pot.

WHAT WE DID INSTEAD.

We could not kill him, so we did the only other thing we could think of.

We went down the stair under the hall and we walled his door up. Stone, then more stone, then earth. We cut warnings into it in three different hands so that nobody would ever mistake it for a cellar.

Then we came up and told the valley it was finished.

That was a lie. All of us knew it was a lie. We told it because people needed to plant wheat.

WHERE TO GO.

Down.

There is a stair under this hall and the sculk has taken it, and sculk does not grow on this side.

Which means the wall did not hold.

The last book is at the bottom, next to the door.
""")

ten = ("10. he has been seen", ADDEXIO, """
10. HE HAS BEEN SEEN.

I am eighty-one years old and I am writing this at the bottom of a stair I helped wall up sixty years ago, beside a hole in that wall that I did not make.

WHAT HAS HAPPENED.

He has been seen.

Twice this spring in the valley. Once at the old farm where these books start. A man walking his fence line at dusk saw somebody standing in the wheat, not breathing.

It is beginning again in the same order it came in the first time. Small things first. Torches out in a line. A door open and still. Prints going one way.

You have been reading these in that order because that is the order it happens in.

WHAT I GOT WRONG.

All of it. Let me be useful and be exact.

I did not believe Pip. Believe the one who comes up the ladder shaking.

We watched the horizon. He is not on the horizon.

We built a prison and let a man with a book into it, and in eleven weeks we did worse to ourselves than he did to Ashfold in three days. We did it because we were frightened and it felt like doing something.

I stayed behind the cart.

And when we could not kill him we hid him, and told everyone it was over, and left it for a stranger to find sixty years later.

That is you.

WHAT YOU HAVE TO DO.

Not what we did.

Do not wall it up. Go through it.

He built a house over there with lights in the windows. A thing that builds a house can be found in it. A thing that can be found can be finished.

Take iron, and take more than you think you need. Do not go alone.

And do not trust a face because it knows your name. Ask it something only a friend would know, and watch the hands.

WHERE I AM.

I am still alive. That surprises me more than anything else in these ten books.

I am under the town, with the last of Ashfold, in the dark where he does not look.

Come down and find me. I will help you as far as I can still walk.

My name is Addexio. I failed at this for sixty years.

Finish it.
""")

HOUSEBOOKS = [
    ("one", one),
    ("two", two),
    ("three", three),
    ("four", four),
    ("five", five),
    ("six", six),
    ("seven", seven),
    ("eight", eight),
    ("nine", nine),
    ("ten", ten),
]

# ══════════════════════════════════════════════ STEVE'S LAB, AT THE TIME
lab_intake = ("the register", STEVE, """
One. Aldous, fletcher, from the east village. Two days. No change.

Two. Hesk, farmer. Four days. No change.

Three. Wendel, cleric. Six days. No change, and he would not stop talking, which I have decided is also no change.

Four. Mila, farmer. Five. Bo, thatcher. Six. Ren, miller. Seven. Sera, weaver. Eight. Gild, the smith's boy, who is fifteen.

No change. No change. No change. No change. No change.

I have written those two words eleven times in this book.

I have started writing them as though they were a disappointment.

They are the only good news in here and I have started resenting them.

I told all eight of them this was work. I paid them for the first week.
""")

lab_door = ("on the door", STEVE, """
It is not a door.

A door has two sides that agree about where they are.

This has one side. Ours.

Whatever is on the other side is not a place. It is a direction, and the direction is toward us.

It gives when he is near it. It does not give when I am.

That is the whole finding after eleven months, and it is the finding I did not want.

So I have built a room around it and filled the room with people who are not him, to find out whether it can tell the difference.

It can.

It has never once opened for a farmer.
""")

lab_nine = ("nine", STEVE, """
Nine is Corin, from the mill road. Sixteen days.

NINE CHANGED.

Not the way Herobrine changed. Slower, and less of it, and then it stopped partway and stayed there.

He is taller than he was. That should not be possible and I have measured him four times.

He does not eat and he does not sleep and he does not blink, and when I stand at the bars he stands at the bars and waits for me to be curious.

Nine answers to his own name.

He answers to mine as well, which is new.

I have written that sentence before. Years ago. About my brother.
""")

lab_lastDay = ("the last day", STEVE, """
The bars are out, not in.

I want that written down because nobody is going to believe it and I will not be here to say it twice.

Corin did not break into anything. He walked OUT.

The others went with him. Aldous, Hesk, Mila, Bo, Ren, Sera, and the smith's boy who is fifteen.

Seven of them, up the shaft, in the dark, without a lamp between them.

Wendel is the only one who stayed and I do not know why and I did not ask.

If you have seen a tall pale thing standing still in the wood, three blocks of it, silent, shaped almost like a person and not quite:

That is Corin. Or it is one of the seven.

They are mine. I made them out of my neighbours in a room I dug myself, and they are still out there, and they are still standing very still, and they are still waiting for somebody to be curious.

I have put the door back. Stone, and more stone, and everything I had left.

It is not going to hold. It was never a door.
""")

lab_plainly = ("plainly, once", STEVE, """
I want this written plainly, one time, and then I am going to stop keeping notes.

I did not do any of this to get Herobrine back.

He was gone before we broke through the hill. I knew it the day he came up. He said my mother's name and my mother had been dead eleven years.

I did it because I wanted to know whether I COULD HAVE.

Whether it could have been my hand on that door. Whether there was something in me that would have come up out of that hole the same way he did.

There was.

That is what I found out. That is the whole of what I found out, and it cost twelve people who thought they were being paid to dig.

Aldous. Hesk. Wendel. Mila. Bo. Ren. Sera. Gild. Corin. And three I did not write down because I had stopped writing them down.

There are three graves at the farm and none of them are theirs.

I know exactly what that means and so do you now.
""")

LABBOOKS = [
    ("intake", lab_intake),
    ("theDoor", lab_door),
    ("subjectNine", lab_nine),
    ("lastDay", lab_lastDay),
    ("whatIWas", lab_plainly),
]
