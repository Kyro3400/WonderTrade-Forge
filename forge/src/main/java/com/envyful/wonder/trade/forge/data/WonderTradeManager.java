package com.envyful.wonder.trade.forge.data;

import com.envyful.api.concurrency.UtilConcurrency;
import com.envyful.api.forge.chat.UtilChatColour;
import com.envyful.api.math.UtilRandom;
import com.envyful.api.player.EnvyPlayer;
import com.envyful.api.reforged.pixelmon.storage.UtilPixelmonPlayer;
import com.envyful.papi.api.util.UtilPlaceholder;
import com.envyful.wonder.trade.forge.WonderTradeForge;
import com.envyful.wonder.trade.forge.config.WonderTradeConfig;
import com.envyful.wonder.trade.forge.data.event.WonderTradeEvent;
import com.google.common.collect.Lists;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.pokemon.PokemonSpec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.io.*;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class WonderTradeManager {

    private final Path tradePoolFile = Paths.get("config/WonderTradeForge/pool.json");
    private final List<Pokemon> tradePool = Lists.newArrayList();

    private final WonderTradeForge mod;

    public WonderTradeManager(WonderTradeForge mod) {
        this.mod = mod;

        File file = tradePoolFile.toFile();

        if (!file.exists()) {
            this.createFile(file);
            this.generatePool();
            this.saveFile(file);
        } else {
            this.loadPool(file);
        }
    }

    private void createFile(File file) {
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void generatePool() {
        for (int i = 0; i < this.mod.getConfig().getNumberInPool(); i++) {
            this.tradePool.add(this.mod.getConfig().getDefaultGeneratorSettings().build());
        }
    }

    public void saveFile() {
        UtilConcurrency.runAsync(() -> this.saveFile(this.tradePoolFile.toFile()));
    }

    private void saveFile(File file) {
        List<PokemonSpec> tradePool = Lists.newArrayList();

        for (Pokemon pokemon : this.tradePool) {
            PokemonSpec spec = new PokemonSpec();
            spec.name = pokemon.getSpecies().name;
            spec.level = pokemon.getLevel();
            spec.shiny = pokemon.isShiny();
            tradePool.add(spec);
        }

        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));

            for (PokemonSpec pokemonSpec : tradePool) {
                ByteBuf buf = Unpooled.buffer();
                pokemonSpec.toBytes(buf);
                bufferedWriter.write(buf.toString(StandardCharsets.UTF_8));
                bufferedWriter.newLine();
            }

            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadPool(File file) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            String line = null;

            while ((line = bufferedReader.readLine()) != null) {
                ByteBuf byteBuf = ByteBufUtil.encodeString(Unpooled.buffer().alloc(),
                        CharBuffer.wrap(line.toCharArray()), StandardCharsets.UTF_8);
                PokemonSpec spec = new PokemonSpec();
                spec.fromBytes(byteBuf);
                this.tradePool.add(spec.create());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void replaceRandomPokemon(EnvyPlayer<EntityPlayerMP> player, Pokemon newPoke) {
        WonderTradeAttribute attribute = player.getAttribute(WonderTradeForge.class);
        Pokemon pokemon = UtilRandom.getRandomElement(this.tradePool);

        WonderTradeEvent event = new WonderTradeEvent(player, newPoke, pokemon);
        MinecraftForge.EVENT_BUS.post(event);

        if (event.isCanceled()) {
            return;
        }

        if (this.shouldBroadcast(newPoke)) {
            for (String broadcast : this.mod.getLocale().getPokemonBroadcast()) {
                FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList()
                        .sendMessage(new TextComponentString(UtilChatColour.translateColourCodes('&',
                                UtilPlaceholder.replaceIdentifiers(player, broadcast))), false);
            }
        }

        UtilPixelmonPlayer.getParty(player.getParent()).set(newPoke.getStorageAndPosition().getSecond(), pokemon);
        this.tradePool.remove(pokemon);
        this.tradePool.add(newPoke);
        player.message(UtilChatColour.translateColourCodes('&',
                UtilPlaceholder.replaceIdentifiers(player, this.mod.getLocale().getTradeSuccessful())));

        UtilConcurrency.runAsync(this::saveFile);
    }

    private boolean shouldBroadcast(Pokemon newPoke) {
        WonderTradeConfig.BroadcastSettings broadcastSettings = this.mod.getConfig().getBroadcastSettings();

        if (broadcastSettings.isAlwaysBroadcast()) {
            return true;
        }

        if (newPoke.isLegendary() && broadcastSettings.isBroadcastLegends()) {
            return true;
        }

        if (newPoke.getSpecies().isUltraBeast() && broadcastSettings.isBroadcastUltraBeasts()) {
            return true;
        }

        return newPoke.isShiny() && broadcastSettings.isBroadcastShinies();
    }
}