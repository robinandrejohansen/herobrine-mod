package com.bloomlet.herobrine;

import com.bloomlet.herobrine.entity.ModEntities;
import com.bloomlet.herobrine.manifest.ManifestationDirector;
import com.bloomlet.herobrine.manifest.Breach;
import com.bloomlet.herobrine.manifest.Cadence;
import com.bloomlet.herobrine.manifest.TheHerd;
import com.bloomlet.herobrine.manifest.Feral;
import com.bloomlet.herobrine.manifest.Nights;
import com.bloomlet.herobrine.manifest.Skies;
import com.bloomlet.herobrine.manifest.Villages;
import com.bloomlet.herobrine.structure.Dwellings;
import com.bloomlet.herobrine.manifest.TheDogKnows;
import com.bloomlet.herobrine.manifest.Journal;
import com.bloomlet.herobrine.manifest.Possession;
import com.bloomlet.herobrine.manifest.Signs;
import com.bloomlet.herobrine.wrath.Wrath;
import com.bloomlet.herobrine.wrath.WrathTriggers;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HerobrineMod implements ModInitializer {
	public static final String MOD_ID = "herobrine";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		// Before everything, because half the registrations below read it.
		Config.load();

		// Must come first: it registers the attachment types, and they have to
		// exist before any world is loaded or saved wrath is discarded.
		Wrath.register();
		Cadence.register();
		Feral.register();
		Skies.register();
		Nights.register();
		TheHerd.register();
		Villages.register();
		Signs.register();
		Journal.register();
		Possession.register();
		com.bloomlet.herobrine.manifest.Mimicry.register();
		Breach.register();
		Dwellings.register();
		TheDogKnows.register();
		ModEntities.register();
		com.bloomlet.herobrine.block.ModBlocks.register();
		WrathTriggers.register();
		ManifestationDirector.register();
		HerobrineCommand.register();

		LOGGER.info("Herobrine is watching.");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
