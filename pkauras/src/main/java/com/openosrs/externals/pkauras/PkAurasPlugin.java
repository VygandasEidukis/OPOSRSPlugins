package com.openosrs.externals.pkauras;

import com.google.inject.Provides;
import com.openosrs.client.util.WeaponMap;
import com.openosrs.client.util.WeaponStyle;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.kit.KitType;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.WorldService;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.HotkeyListener;
import org.pf4j.Extension;
import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Extension
@PluginDescriptor(
        name = "Pk auras helper",
        description = "Swap aura from target"
)
@Slf4j
public class PkAurasPlugin extends Plugin
{
    @Inject
    private ChatMessageManager chatMessageManager;

    @Inject
    private PkAurasConfig config;
    @Inject
    private Client client;
    @Inject
    private EventBus eventBus;
    @Inject
    private WorldService worldService;
    @Inject
    private KeyManager keyManager;

    @Inject
    private ClientThread clientThread;
    private boolean run = true;
    private ExecutorService executor;
    private WeaponStyle currentEnemyCombatType = WeaponStyle.MAGIC;
    private WeaponStyle currentLocalCombatType = WeaponStyle.MAGIC;
    private Player currentOpponent;
    private Timer cdTimer;
    private Timer agroTimer;

    private int localCd;
    private final ActionListener agroTimerAction = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            if (agroTimer == null)
            {
                return;
            }

            if (localCd > 0)
            {
                localCd--;
            }

            agroTimer.setDelay(getMillis());
            agroTimer.restart();
        }
    };

    private int cooldown;
    private final ActionListener cdTimerAction = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            if (cdTimer == null)
            {
                return;
            }

            if (cooldown > 0)
            {
                cooldown--;
            }

            cdTimer.setDelay(getMillisPraySwap());
            cdTimer.restart();
        }
    };

    @Provides
    PkAurasConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(PkAurasConfig.class);
    }

    @Subscribe
    private void onAnimationChanged(AnimationChanged animationChanged)
    {
        if ((animationChanged.getActor() instanceof Player) && (animationChanged.getActor().getInteracting() instanceof Player) && (animationChanged.getActor().getInteracting() == client.getLocalPlayer()))
        {
            currentOpponent = (Player) animationChanged.getActor();
        }
    }
    @Subscribe
    private void onInteractingChanged(InteractingChanged interactingChanged)
    {
        if ((interactingChanged.getSource() instanceof Player) && (interactingChanged.getTarget() instanceof Player))
        {
            Player targetPlayer = (Player) interactingChanged.getTarget();
            if (targetPlayer == client.getLocalPlayer())
            {
                currentOpponent = (Player) interactingChanged.getSource();
            }
        }
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event)
    {
        processRequiredPrayer();
        executeOffensivePrayer();
    }

    private final HotkeyListener toggle = new HotkeyListener(() -> config.toggle())
    {
        @Override
        public void hotkeyPressed()
        {
            run = !run;
            log.info("Activated: " + run);
            if (run)
            {
                sendGameMessage(":)");
                cooldown = 0;
                localCd = 0;
                cdTimer.start();
                agroTimer.start();
            }
            else
            {
                cooldown = 99999;
                localCd = 9999;
                cdTimer.stop();
                agroTimer.stop();
                sendGameMessage(":(");
            }
        }
    };

    private MenuEntry entry;

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event)
    {
        if (entry != null)
        {
            event.setMenuEntry(entry);
        }
        entry = null;
    }

    @Subscribe
    private void onPlayerDespawned(PlayerDespawned playerDespawned)
    {
        if (playerDespawned.getPlayer() == currentOpponent || playerDespawned.getPlayer() == client.getLocalPlayer())
        {
            currentOpponent = null;
        }
    }

    public void sendGameMessage(String message) {
        String chatMessage = new ChatMessageBuilder()
                .append(ChatColorType.HIGHLIGHT)
                .append(message)
                .build();

        chatMessageManager
                .queue(QueuedMessage.builder()
                        .type(ChatMessageType.CONSOLE)
                        .runeLiteFormattedMessage(chatMessage)
                        .build());
    }

    @Subscribe
    private void onClientTick(ClientTick clientTick)
    {
        if (cooldown > 0 || currentOpponent == null)
        {
            return;
        }

        processRequiredPrayer();

        if (cooldown <= 0)
        {
            switch (currentEnemyCombatType)
            {
                case MAGIC:
                    activatePrayer(Prayer.PROTECT_FROM_MAGIC);
                    break;
                case MELEE:
                    activatePrayer(Prayer.PROTECT_FROM_MELEE);
                    break;
                case RANGE:
                    activatePrayer(Prayer.PROTECT_FROM_MISSILES);
                    break;
            }

            cooldown = 1;
        }

        if (localCd <= 0)
        {
            executeOffensivePrayer();
            localCd = 1;
        }
    }

    private void executeOffensivePrayer() {
        switch (currentLocalCombatType)
        {
            case MAGIC:
                activatePrayer(Prayer.AUGURY);
                break;
            case MELEE:
                activatePrayer(Prayer.PIETY);
                break;
            case RANGE:
                activatePrayer(Prayer.RIGOUR);
                break;
        }
    }

    public void activatePrayer(Prayer prayer)
    {
        if (prayer == null)
        {
            return;
        }

        if (client.isPrayerActive(prayer))
        {
            return;
        }

        WidgetInfo widgetInfo = prayer.getWidgetInfo();

        if (widgetInfo == null)
        {
            return;
        }

        Widget prayer_widget = client.getWidget(widgetInfo);

        if (prayer_widget == null)
        {
            return;
        }

        if (client.getBoostedSkillLevel(Skill.PRAYER) <= 0)
        {
            return;
        }

        log.info("Swapped to: " + prayer.name());
        client.invokeMenuAction("Activate",
                prayer_widget.getName(),
                1,
                MenuAction.CC_OP.getId(),
                prayer_widget.getItemId(),
                prayer_widget.getId());
    }


    @Override
    protected void startUp() throws AWTException
    {
        executor = Executors.newSingleThreadExecutor();
        keyManager.registerKeyListener(toggle);
        cdTimer = new Timer(getMillisPraySwap(), cdTimerAction);
        agroTimer = new Timer(getMillis(), agroTimerAction);
        run = true;
    }

    @Override
    protected void shutDown()
    {
        executor.shutdown();
        keyManager.unregisterKeyListener(toggle);
        cdTimer = null;
        run = false;
    }

    private void processRequiredPrayer()
    {
        if (currentOpponent == null)
        {
            return;
        }

        client.getLocalPlayerIndex();
        int itemId = currentOpponent.getPlayerComposition().getEquipmentId(KitType.WEAPON);
        currentEnemyCombatType = getCombatType(itemId);

        var localPlayer = client.getLocalPlayer();
        itemId = localPlayer.getPlayerComposition().getEquipmentId(KitType.WEAPON);
        currentLocalCombatType = getCombatType(itemId);
    }

    private WeaponStyle getCombatType(int weaponId)
    {
        String weaponStringId = Integer.toString(weaponId);

        return WeaponMap.StyleMap.get(weaponId);
    }

    public int getMillis()
    {
        return (int) (Math.random() * config.randLow() + config.randHigh());
    }

    public int getMillisPraySwap()
    {
        return (int) (Math.random() * config.randLowSwapPrayer() + config.randHighSwapPrayer());
    }
}