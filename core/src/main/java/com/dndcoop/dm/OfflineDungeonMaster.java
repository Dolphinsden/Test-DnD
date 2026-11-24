package com.dndcoop.dm;

import com.dndcoop.campaign.AshenLanternCampaign;
import com.dndcoop.campaign.EncounterTemplate;
import com.dndcoop.campaign.LocationNode;
import com.dndcoop.campaign.NpcProfile;
import com.dndcoop.model.Ability;
import com.dndcoop.model.CampaignState;
import com.dndcoop.model.CharacterSheet;
import com.dndcoop.model.PendingRoll;
import com.dndcoop.model.Skill;
import com.dndcoop.rules.RulesEngine;

import java.util.Map;

public class OfflineDungeonMaster {

    public DmDirective processExplorationAction(CampaignState state, CharacterSheet actor,
                                                String freeText, RulesEngine rules) {
        DmDirective dir = new DmDirective();
        dir.setDmSourceLabel("Offline DM");
        dir.setUsedFallback(true);

        String text = freeText == null ? "" : freeText.trim();
        String lower = text.toLowerCase();
        LocationNode loc = AshenLanternCampaign.getLocation(state.getCurrentLocationId());

        // 1. Resting
        if (containsAny(lower, "long rest", "sleep", "camp for the night", "rest for the night")) {
            if (loc.isSafeForLongRest() || state.getDefeatedEncounters().contains(loc.getEncounterId())) {
                dir.setRestType("long");
                dir.setNarrative(actor.getName() + " organizes watches as the party settles in for a full Long Rest at " + loc.getName() + ".");
            } else {
                dir.setNarrative("The air in " + loc.getName() + " feels far too perilous for an 8-hour Long Rest right now. Try a Short Rest, clear nearby threats, or fall back to a safer refuge like The Gilded Stag Tavern or the Weeping Shrine.");
            }
            return dir;
        }
        if (containsAny(lower, "short rest", "catch our breath", "take a breather", "bandage", "rest")) {
            dir.setRestType("short");
            dir.setNarrative(actor.getName() + " calls for a quiet hour to tend wounds, sharpen blades, and catch everyone's breath.");
            return dir;
        }

        // 2. Travel / Movement to exits
        String matchedExit = matchExit(loc, lower);
        if (matchedExit != null) {
            String requiredFlag = loc.getLockedExitRequiredFlags().get(matchedExit);
            if (requiredFlag != null && !state.hasFlag(requiredFlag)) {
                if ("has_sun_key".equals(requiredFlag)) {
                    // Check if someone in the party has the Bronze Sun-Key item!
                    boolean hasKeyItem = false;
                    for (CharacterSheet h : state.getParty()) {
                        if (h.findItemById("bronze_sun_key") != null) {
                            hasKeyItem = true;
                            break;
                        }
                    }
                    if (hasKeyItem || state.hasFlag("cleared_ossuary_patrol")) {
                        state.setFlag("has_sun_key", true);
                    } else {
                        dir.setNarrative("The colossal Northern Sanctum Gate stands sealed by a gleaming sun-shaped keyhole. You'll need the Bronze Sun-Key from the Ossuary Cloister to the east before this gate will yield.");
                        return dir;
                    }
                } else {
                    dir.setNarrative("The way forward is still barred—you must overcome the challenge here first!");
                    return dir;
                }
            }
            dir.setMoveToLocationId(matchedExit);
            LocationNode dest = AshenLanternCampaign.getLocation(matchedExit);
            dir.setNarrative(actor.getName() + " leads the party onward toward " + dest.getName() + ".");
            return dir;
        }

        // 3. Talking to NPCs in this location
        for (NpcProfile npc : loc.getNpcs()) {
            String firstWord = npc.getName().toLowerCase().split("\\s+")[0];
            if (lower.contains(firstWord) || containsAny(lower, "talk", "ask", "speak", "chat", "innkeeper", "guard", "captain", "apothecary", "persuade", "question")) {
                if (lower.contains(firstWord) || loc.getNpcs().size() == 1 || containsAny(lower, "talk", "ask", "speak", "persuade")) {
                    // Pick the most relevant NPC if multiple
                    NpcProfile chosen = pickBestNpc(loc, lower);
                    if (chosen != null) {
                        if (!state.hasFlag(chosen.getFlagGranted()) && containsAny(lower, "secret", "more", "clue", "persuade", "convince", "help", "reward", "potion", "truth", "insight", "lantern", "ask", "tell")) {
                            PendingRoll pr = rules.buildAbilityCheckRoll(
                                    actor, chosen.getPersuadeSkill(), chosen.getPersuadeSkill().getGoverningAbility(),
                                    chosen.getPersuadeDc(),
                                    actor.getName() + " presses " + chosen.getName() + " (" + chosen.getPersuadeSkill().getDisplayName() + " DC " + chosen.getPersuadeDc() + ")",
                                    text);
                            pr.setSuccessNarrative(chosen.getSecretInfo());
                            pr.setFailureNarrative(chosen.getGreeting() + "\n" + chosen.getName() + " hesitates, holding back any further details for now.");
                            pr.setFlagOnSuccess(chosen.getFlagGranted());
                            pr.setRewardItemOnSuccess(chosen.getRewardItemId());
                            pr.setXpOnSuccess(40);
                            dir.setNarrative(chosen.getName() + " (" + chosen.getTitle() + ") meets " + actor.getName() + "'s gaze: " + chosen.getGreeting());
                            dir.setRequestedRoll(pr);
                            return dir;
                        }
                        dir.setNarrative(chosen.getName() + " (" + chosen.getTitle() + ") responds to " + actor.getName() + ":\n" + chosen.getGreeting()
                                + (state.hasFlag(chosen.getFlagGranted()) ? "\n\n" + chosen.getSecretInfo() : ""));
                        return dir;
                    }
                }
            }
        }

        // 4. Starting / Confronting undefeated encounter in this location
        if (loc.getEncounterId() != null && !state.getDefeatedEncounters().contains(loc.getEncounterId())) {
            if (containsAny(lower, "attack", "fight", "charge", "ambush", "confront", "engage", "malakor", "goblin", "skeleton", "monster", "battle", "draw weapon", "strike")) {
                EncounterTemplate enc = AshenLanternCampaign.getEncounter(loc.getEncounterId());
                dir.setStartEncounterId(enc.getId());
                dir.setNarrative(actor.getName() + " steps forward boldly, steel drawn, triggering a clash!");
                return dir;
            }
        }

        // 5. Location Discovery / Trap / Lockpick / Shrine / Tracks check
        if (loc.getInvestigateFlag() != null && !state.hasFlag(loc.getInvestigateFlag())
                && containsAny(lower, "search", "inspect", "examine", "investigate", "look", "track", "trap", "lock", "pick", "chest", "rune", "shrine", "noticeboard", "pedestal", "relief", "disarm", "cleanse", "pray", "open", "loot", "ritual", "circle", "force", "smash")) {
            Skill skill = loc.getInvestigateSkill();
            if (containsAny(lower, "smash", "force", "break") && "warden_vault".equals(loc.getId())) {
                skill = Skill.ATHLETICS;
            } else if (containsAny(lower, "pray", "bless", "religion") && ("weeping_shrine".equals(loc.getId()) || "ossuary_cloister".equals(loc.getId()))) {
                skill = Skill.RELIGION;
            }
            PendingRoll pr = rules.buildAbilityCheckRoll(actor, skill, skill.getGoverningAbility(),
                    loc.getInvestigateDc(),
                    actor.getName() + " attempts " + skill.getDisplayName() + " in " + loc.getName() + " (DC " + loc.getInvestigateDc() + ")",
                    text);
            pr.setSuccessNarrative(loc.getInvestigateSuccessText());
            pr.setFailureNarrative(loc.getInvestigateFailureText());
            pr.setFlagOnSuccess(loc.getInvestigateFlag());
            pr.setRewardItemOnSuccess(loc.getInvestigateRewardItemId());
            pr.setGoldOnSuccess(loc.getInvestigateGold());
            pr.setXpOnSuccess(loc.getInvestigateXp());
            dir.setNarrative(actor.getName() + " carefully acts upon \"" + text + "\" inside " + loc.getName() + "...");
            dir.setRequestedRoll(pr);
            return dir;
        }

        // 6. Epilogue completion at Rekindled Spire or Tavern after defeating Malakor
        if (state.hasFlag("defeated_malakor") && ("lantern_altar".equals(loc.getId()) || "gilded_stag_tavern".equals(loc.getId()))
                && containsAny(lower, "lantern", "claim", "rekindle", "return", "celebrate", "light", "finish", "complete", "toast", "victory")) {
            state.setCampaignCompleted(true);
            dir.setNarrative("The Ashen Lantern blazes with pure golden Dawnfire! Across Hollowbrook, the bog-mist retreats into the night as villagers cheer your names from the cobblestones. Martha raises a mug at The Gilded Stag: \"To the saviors of Hollowbrook!\"\n\n*** CAMPAIGN COMPLETE! *** You may keep roleplaying, explore remaining rooms, or export your Markdown Adventure Chronicle from the top bar!");
            return dir;
        }

        // 7. Look around / scene summary
        if (containsAny(lower, "look around", "where are we", "describe", "survey", "status")) {
            dir.setNarrative(loc.getDescription() + "\n\nHint: " + loc.getAmbientHint());
            return dir;
        }

        // 8. Generic free-form skill detection so ANY creative player text feels responsive with 5E checks!
        Skill detectedSkill = inferSkillFromFreeText(lower);
        if (detectedSkill != null) {
            int dc = 12;
            PendingRoll pr = rules.buildAbilityCheckRoll(actor, detectedSkill, detectedSkill.getGoverningAbility(), dc,
                    actor.getName() + " — " + detectedSkill.getDisplayName() + " Check (DC " + dc + ")", text);
            pr.setSuccessNarrative("With practiced skill, " + actor.getName() + " pulls off \"" + text
                    + "\" cleanly! The party gains momentum and sharper awareness of " + loc.getName() + ".");
            pr.setFailureNarrative(actor.getName() + " tries \"" + text
                    + "\", but the damp shadows of " + loc.getName() + " complicate the effort—nothing catastrophic, though a new approach may be needed.");
            pr.setXpOnSuccess(15);
            dir.setNarrative("The DM considers " + actor.getName() + "'s bold move (\"" + text
                    + "\") and calls for a " + detectedSkill.getDisplayName() + " check!");
            dir.setRequestedRoll(pr);
            return dir;
        }

        // 9. Pure atmospheric narrative response
        dir.setNarrative(actor.getName() + " acts in " + loc.getName() + ": \"" + text + "\"\n\n"
                + "The flickering light shifts across the scene as your companions watch intently. "
                + loc.getAmbientHint());
        return dir;
    }

    private NpcProfile pickBestNpc(LocationNode loc, String lower) {
        for (NpcProfile npc : loc.getNpcs()) {
            String first = npc.getName().toLowerCase().split("\\s+")[0];
            if (lower.contains(first) || lower.contains(npc.getTitle().toLowerCase().split("\\s+")[0])) {
                return npc;
            }
        }
        return loc.getNpcs().isEmpty() ? null : loc.getNpcs().get(0);
    }

    private String matchExit(LocationNode loc, String lower) {
        boolean travelIntent = containsAny(lower, "go ", "walk", "head ", "move", "enter", "travel", "leave",
                "exit", "return", "step ", "climb", "descend", "ascend", "approach", "north", "south", "east", "west", "outside", "inside");
        for (Map.Entry<String, String> entry : loc.getExits().entrySet()) {
            String targetId = entry.getKey();
            String label = entry.getValue().toLowerCase();
            LocationNode dest = AshenLanternCampaign.getLocation(targetId);
            String destName = dest.getName().toLowerCase();

            if (lower.contains(targetId.replace('_', ' ')) || lower.contains(destName)) {
                return targetId;
            }
            if (travelIntent) {
                for (String kw : new String[]{"north", "south", "east", "west", "outside", "tavern", "square",
                        "mire", "boardwalk", "shrine", "steps", "reliquary", "hall", "ossuary", "vault", "sanctum", "spire"}) {
                    if (lower.contains(kw) && (label.contains(kw) || destName.contains(kw))) {
                        return targetId;
                    }
                }
            }
        }
        return null;
    }

    private Skill inferSkillFromFreeText(String lower) {
        if (containsAny(lower, "sneak", "stealth", "creep", "hide", "quietly", "tiptoe")) return Skill.STEALTH;
        if (containsAny(lower, "climb", "jump", "lift", "shove", "push", "swim", "kick down", "break")) return Skill.ATHLETICS;
        if (containsAny(lower, "flip", "balance", "tumble", "dodge", "vault over")) return Skill.ACROBATICS;
        if (containsAny(lower, "listen", "spot", "watch", "scan", "peer", "perceive")) return Skill.PERCEPTION;
        if (containsAny(lower, "magic", "arcane", "spell", "glow", "rune", "enchant")) return Skill.ARCANA;
        if (containsAny(lower, "pray", "god", "temple", "undead", "holy", "altar")) return Skill.RELIGION;
        if (containsAny(lower, "intimidate", "threaten", "roar", "scare", "glare")) return Skill.INTIMIDATION;
        if (containsAny(lower, "lie", "bluff", "deceive", "disguise", "pretend")) return Skill.DECEPTION;
        if (containsAny(lower, "persuade", "bargain", "haggle", "charm", "convince")) return Skill.PERSUASION;
        if (containsAny(lower, "sing", "lute", "play music", "dance", "perform", "story")) return Skill.PERFORMANCE;
        if (containsAny(lower, "heal", "medicine", "diagnose", "stabilize", "wound")) return Skill.MEDICINE;
        if (containsAny(lower, "pickpocket", "steal", "palm", "lockpick", "tinker")) return Skill.SLEIGHT_OF_HAND;
        if (containsAny(lower, "track", "forage", "trail", "footprints", "weather")) return Skill.SURVIVAL;
        if (containsAny(lower, "history", "legend", "lore", "inscription", "statue")) return Skill.HISTORY;
        if (containsAny(lower, "herb", "plant", "beast", "animal", "marsh", "bog")) return Skill.NATURE;
        if (containsAny(lower, "read their face", "motive", "trust", "sense", "insight")) return Skill.INSIGHT;
        return null;
    }

    private boolean containsAny(String haystack, String... needles) {
        for (String n : needles) {
            if (haystack.contains(n)) {
                return true;
            }
        }
        return false;
    }
}
