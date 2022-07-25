package com.cavetale.tetris;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandNode;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.playercache.PlayerCache;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class AdminCommand extends AbstractCommand<TetrisPlugin> {
    protected AdminCommand(final TetrisPlugin plugin) {
        super(plugin, "tetrisadmin");
    }

    @Override
    protected void onEnable() {
        rootNode.addChild("list").denyTabCompletion()
            .description("List games")
            .senderCaller(this::list);
        rootNode.addChild("shift").denyTabCompletion()
            .description("Shift up")
            .playerCaller(this::shift);
        rootNode.addChild("educate").denyTabCompletion()
            .description("Spam keybinds")
            .playerCaller(this::educate);
        rootNode.addChild("korobeniki").denyTabCompletion()
            .description("Play korobeniki")
            .playerCaller(p -> Korobeniki.play(p));
        CommandNode tournamentNode = rootNode.addChild("tournament")
            .description("Tournament commands");
        tournamentNode.addChild("enable").denyTabCompletion()
            .description("Enable the tournament")
            .senderCaller(this::tournamentEnable);
        tournamentNode.addChild("disable").denyTabCompletion()
            .description("Disable the tournament")
            .senderCaller(this::tournamentDisable);
        tournamentNode.addChild("auto").denyTabCompletion()
            .description("Toggle auto mode")
            .senderCaller(this::tournamentAuto);
        tournamentNode.addChild("rank").arguments("<player> <amount>")
            .description("Adjust player rank")
            .completers(CommandArgCompleter.NULL, CommandArgCompleter.integer(i -> i != 0))
            .senderCaller(this::tournamentRank);
        tournamentNode.addChild("clear").denyTabCompletion()
            .description("Clear the rankings")
            .senderCaller(this::tournamentClear);
        tournamentNode.addChild("reward").denyTabCompletion()
            .description("Deliver rewards")
            .senderCaller(this::tournamentReward);
        CommandNode battleNode = rootNode.addChild("battle")
            .description("Multiplayer battle commands");
        battleNode.addChild("start").arguments("<player...>")
            .description("Start a battle")
            .completers(CommandArgCompleter.NULL, CommandArgCompleter.REPEAT)
            .senderCaller(this::battleStart);
    }

    private void list(CommandSender sender) {
        sender.sendMessage(text("" + plugin.games.size() + " games:", YELLOW));
        for (TetrisGame game : plugin.games) {
            sender.sendMessage(text("- " + game.getPlayer().getName() + ": " + game.getState() + " " + game.getScore()));
        }
    }

    private void shift(Player player) {
        TetrisPlayer session = plugin.sessions.of(player);
        if (session.getGame() == null || !session.getGame().isActive()) {
            throw new CommandWarn("Game not active!");
        }
        session.getGame().shiftUp();
        player.sendMessage(text("Shifted up", YELLOW));
    }

    private void educate(Player player) {
        TetrisGame.educate(player);
    }

    private void tournamentEnable(CommandSender sender) {
        if (plugin.getTournament() != null) {
            throw new CommandWarn("Tournament already enabled!");
        }
        Tournament tournament = new Tournament(plugin);
        plugin.setTournament(tournament);
        tournament.enable();
        sender.sendMessage(text("Tournament enabled", AQUA));
    }

    private void tournamentDisable(CommandSender sender) {
        Tournament tournament = plugin.getTournament();
        if (tournament == null) {
            throw new CommandWarn("Tournament not enabled!");
        }
        tournament.disable();
        plugin.setTournament(null);
        sender.sendMessage(text("Tournament disabled", RED));
    }

    private void tournamentAuto(CommandSender sender) {
        Tournament tournament = plugin.getTournament();
        if (tournament == null) {
            throw new CommandWarn("Tournament not enabled!");
        }
        boolean auto = !tournament.isAuto();
        tournament.setAuto(auto);
        tournament.save();
        if (auto) {
            sender.sendMessage(text("Auto mode enabled", AQUA));
        } else {
            sender.sendMessage(text("Auto mode disabled", RED));
        }
    }

    private boolean tournamentRank(CommandSender sender, String[] args) {
        if (args.length != 2) return false;
        if (plugin.getTournament() == null) {
            throw new CommandWarn("Tournament not enabled!");
        }
        PlayerCache target = PlayerCache.require(args[0]);
        int amount = CommandArgCompleter.requireInt(args[1], i -> i != 0);
        plugin.getTournament().addRank(target.uuid, amount);
        plugin.getTournament().save();
        plugin.getTournament().computeHighscore();
        sender.sendMessage(text("Rank of " + target.getName() + " adjusted by " + amount, AQUA));
        return true;
    }

    private void tournamentClear(CommandSender sender) {
        if (plugin.getTournament() == null) {
            throw new CommandWarn("Tournament not enabled!");
        }
        plugin.getTournament().getTag().getRanks().clear();
        plugin.getTournament().save();
        plugin.getTournament().computeHighscore();
        sender.sendMessage(text("Ranks cleared!", AQUA));
    }

    private void tournamentReward(CommandSender sender) {
        if (plugin.getTournament() == null) {
            throw new CommandWarn("Tournament not enabled!");
        }
        int count = plugin.getTournament().reward();
        sender.sendMessage(text(count + " rewards given"));
    }

    private boolean battleStart(CommandSender sender, String[] args) {
        if (args.length < 2) return false;
        List<Player> players = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (String arg : args) {
            Player player = Bukkit.getPlayerExact(arg);
            if (player == null) throw new CommandWarn("Player not found: " + arg);
            if (players.contains(player)) throw new CommandWarn("Duplicate player: " + player.getName());
            players.add(player);
            names.add(player.getName());
        }
        TetrisBattle battle = new TetrisBattle();
        for (Player player : players) {
            TetrisGame game = plugin.startGame(player);
            battle.getGames().add(game);
            game.setBattle(battle);
            player.sendMessage(text("Starting battle with " + String.join(", ", names), GREEN));
        }
        sender.sendMessage(text("Battle started: " + String.join(", ", names), AQUA));
        return true;
    }
}
