package com.patchpayment;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Map;
import java.util.Set;

import static net.runelite.api.ItemID.*;

@PluginDescriptor(
        name = "Patch Payment",
        description = "Creates a menu item to check a seed's required payment.",
        tags = {"farming", "payment"}
)
@Slf4j
public class PatchPaymentPlugin extends Plugin {
    static final String CONFIG_GROUP = "patchpayment";
    static final String CHECK_EXAMINE = "examinetext";
    static final String CHECK_BANK = "bankmenuitem";
    static final String CHECK_VAULT = "seedvaultitem";
    private static final String CHECK_PAYMENT = "Check";
    private static final String EXAMINE = "Examine";

    private static final Map<Integer, PairInterface> payments;

    private final Set<Integer> acceptedWidgetIds = Sets.newHashSet();

    private static void add(ImmutableMap.Builder<Integer, PairInterface> builder, PairInterface response)
    {
        for(int id : response.getTargetItems())
        {
            builder.put(id, response);
        }
    }

    static {
        ImmutableMap.Builder<Integer, PairInterface> builder = new ImmutableMap.Builder<>();

        add(builder, new PaymentPair("2 buckets of compost", new int[] {POTATO_SEED}));
        add(builder, new PaymentPair("1 full sack of potatoes", new int[] {ONION_SEED}));
        add(builder, new PaymentPair("1 full sack of onions", new int[] {CABBAGE_SEED}));
        add(builder, new PaymentPair("2 full sacks of cabbages", new int[] {TOMATO_SEED}));
        add(builder, new PaymentPair("10 jute fibres", new int[] {SWEETCORN_SEED}));
        add(builder, new PaymentPair("1 full basket of apples", new int[] {STRAWBERRY_SEED}));
        add(builder, new PaymentPair("10 curry leaves", new int[] {WATERMELON_SEED}));
        add(builder, new PaymentPair("5 jangerberries", new int[] {SNAPE_GRASS_SEED}));
        // FLOWER SEEDS
        add(builder, new CustomPair(new int[] {MARIGOLD_SEED, ROSEMARY_SEED, NASTURTIUM_SEED, WOAD_SEED, LIMPWURT_SEED, WHITE_LILY_SEED}));
        // HERB PAIRS
        add(builder, new CustomPair(new int[] {GUAM_SEED, MARRENTILL_SEED, TARROMIN_SEED, HARRALANDER_SEED, GOUT_TUBER, RANARR_SEED, TOADFLAX_SEED, IRIT_SEED, AVANTOE_SEED, KWUARM_SEED, SNAPDRAGON_SEED, HUASCA_SEED, CADANTINE_SEED, LANTADYME_SEED, DWARF_WEED_SEED, TORSTOL_SEED}));
        // HOPS PAIRS
        add(builder, new PaymentPair("3 buckets of compost", new int[] {BARLEY_SEED}));
        add(builder, new PaymentPair("1 marigold", new int[] {HAMMERSTONE_SEED}, "hammerstone hops"));
        add(builder, new PaymentPair("1 full sack of onions", new int[] {ASGARNIAN_SEED}, "asgarnian hops"));
        add(builder, new PaymentPair("6 barley malts", new int[] {JUTE_SEED}));
        add(builder, new PaymentPair("1 full basket of tomatoes", new int[] {YANILLIAN_SEED}, "yanillian hops"));
        add(builder, new PaymentPair("3 full sacks of cabbages", new int[] {KRANDORIAN_SEED}, "krandorian hops"));
        add(builder, new PaymentPair("1 nasturtium", new int[] {WILDBLOOD_SEED}));
        // BUSH PAIRS
        add(builder, new PaymentPair("4 full sacks of cabbages", new int[] {REDBERRY_SEED}));
        add(builder, new PaymentPair("3 full baskets of tomatoes", new int[] {CADAVABERRY_SEED}));
        add(builder, new PaymentPair("3 full baskets of strawberries", new int[] {DWELLBERRY_SEED}));
        add(builder, new PaymentPair("6 watermelons", new int[] {JANGERBERRY_SEED}));
        add(builder, new PaymentPair("8 bittercap mushrooms", new int[] {WHITEBERRY_SEED}));
        add(builder, new CustomPair("as it will never get diseased", new int[] {POISON_IVY_SEED}));
        // TREE PAIRS
        add(builder, new PaymentPair("1 full basket of tomatoes", new int[] {ACORN, OAK_SAPLING, OAK_SEEDLING, OAK_SEEDLING_W}, "oak tree"));
        add(builder, new PaymentPair("1 full basket of apples", new int[] {WILLOW_SEED, WILLOW_SAPLING, WILLOW_SEEDLING, WILLOW_SEEDLING_W}, "willow tree"));
        add(builder, new PaymentPair("1 full basket of oranges", new int[] {MAPLE_SEED, MAPLE_SAPLING, MAPLE_SEEDLING, MAPLE_SEEDLING_W}, "maple tree"));
        add(builder, new PaymentPair("10 cactus spines", new int[] {YEW_SEED, YEW_SAPLING, YEW_SEEDLING, YEW_SEEDLING_W}, "yew tree"));
        add(builder, new PaymentPair("25 coconuts", new int[] {MAGIC_SEED, MAGIC_SAPLING, MAGIC_SEEDLING, MAGIC_SEEDLING_W}, "magic tree"));
        // FRUIT TREE PAIRS
        add(builder, new PaymentPair("9 sweetcorn", new int[] {APPLE_TREE_SEED, APPLE_SAPLING, APPLE_SEEDLING, APPLE_SEEDLING_W}, "apple tree"));
        add(builder, new PaymentPair("4 full baskets of apples", new int[] {BANANA_TREE_SEED, BANANA_SAPLING, BANANA_SEEDLING, BANANA_SEEDLING_W}, "banana tree"));
        add(builder, new PaymentPair("3 full baskets of strawberries", new int[] {ORANGE_TREE_SEED, ORANGE_SAPLING, ORANGE_SEEDLING, ORANGE_SEEDLING_W}, "orange tree"));
        add(builder, new PaymentPair("5 full baskets of bananas", new int[] {CURRY_TREE_SEED, CURRY_SAPLING, CURRY_SEEDLING, CURRY_SEEDLING_W}, "curry tree"));
        add(builder, new PaymentPair("10 watermelons", new int[] {PINEAPPLE_SEED, PINEAPPLE_SAPLING, PINEAPPLE_SEEDLING, PINEAPPLE_SEEDLING_W}));
        add(builder, new PaymentPair("10 pineapples", new int[] {PAPAYA_TREE_SEED, PAPAYA_SAPLING, PAPAYA_SEEDLING, PAPAYA_SEEDLING_W}, "papaya tree"));
        add(builder, new PaymentPair("15 papaya fruit", new int[] {PALM_TREE_SEED, PALM_SAPLING, PALM_SEEDLING, PALM_SEEDLING_W}, "palm tree"));
        add(builder, new PaymentPair("15 coconuts", new int[] {DRAGONFRUIT_TREE_SEED, DRAGONFRUIT_SAPLING, DRAGONFRUIT_SEEDLING, DRAGONFRUIT_SEEDLING_W}, "dragonfruit tree"));
        // SPECIAL PAIRS
        add(builder, new PaymentPair("200 numulite", new int[] {SEAWEED_SPORE}));
        add(builder, new CustomPair("as it is protected for free. That doesn't make sense but I'm too lazy to change it", new int[] {GRAPE_SEED}));
        add(builder, new CustomPair(new int[] {MUSHROOM_SPORE, BELLADONNA_SEED}));
        add(builder, new CustomPair("as it is immune to disease", new int[] {HESPORI_SEED}));
        // ANIMA PAIRS
        add(builder, new CustomPair(new int[] {KRONOS_SEED, IASOR_SEED, ATTAS_SEED}));
        // SPECIAL TREE PAIRS
        add(builder, new PaymentPair("15 limpwurt roots", new int[] {TEAK_SEED, TEAK_SAPLING, TEAK_SEEDLING, TEAK_SEEDLING_W}, "teak tree"));
        add(builder, new PaymentPair("25 yanillian hops", new int[] {MAHOGANY_SEED, MAHOGANY_SAPLING, MAHOGANY_SEEDLING, MAHOGANY_SEEDLING_W}, "mahogany tree"));
        add(builder, new PaymentPair("8 poison ivy berries", new int[] {CALQUAT_TREE_SEED, CALQUAT_SAPLING, CALQUAT_SEEDLING, CALQUAT_SEEDLING_W}, "calquat tree"));
        add(builder, new CustomPair("as it is immune to disease", new int[] {CRYSTAL_SEED, CRYSTAL_SAPLING, CRYSTAL_SEEDLING, CRYSTAL_SEEDLING_W}));
        add(builder, new PaymentPair("5 monkey nuts, 1 monkey bar, and 1 ground tooth", new int[] {SPIRIT_SEED, SPIRIT_SAPLING, SPIRIT_SEEDLING, SPIRIT_SEEDLING_W}, "spirit tree"));
        add(builder, new PaymentPair("8 potato cacti", new int[] {CELASTRUS_SEED, CELASTRUS_SAPLING, CELASTRUS_SEEDLING, CELASTRUS_SEEDLING_W}, "celastrus tree"));
        add(builder, new PaymentPair("6 dragonfruit", new int[] {REDWOOD_TREE_SEED, REDWOOD_SAPLING, REDWOOD_SEEDLING, REDWOOD_SEEDLING_W}, "redwood tree"));
        // CACTI PAIRS
        add(builder, new PaymentPair("6 cadava berries", new int[] {CACTUS_SEED}));
        add(builder, new PaymentPair("8 snape grass", new int[] {POTATO_CACTUS_SEED}));

        payments = builder.build();
    }

    @Inject
    @Nullable
    private Client client;

    @Inject
    private ChatMessageManager chatMessageManager;
    @Inject
    private PatchPaymentConfig config;

    @Provides
    PatchPaymentConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(PatchPaymentConfig.class);
    }

    @Override
    protected void startUp(){
        updateConfig();
    }

    @Override
    protected void shutDown()
    {

    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if(!event.getGroup().equals(CONFIG_GROUP)) return;
        updateConfig();
    }

    private void updateConfig()
    {
        acceptedWidgetIds.clear();
        acceptedWidgetIds.add(ComponentID.INVENTORY_CONTAINER);

        if(config.checkInVault())
        {
            acceptedWidgetIds.add(ComponentID.SEED_VAULT_ITEM_CONTAINER);
            acceptedWidgetIds.add(ComponentID.SEED_VAULT_INVENTORY_ITEM_CONTAINER);
        }

        if(config.checkInBank())
        {
            acceptedWidgetIds.add(ComponentID.BANK_ITEM_CONTAINER);
            acceptedWidgetIds.add(ComponentID.BANK_INVENTORY_ITEM_CONTAINER);
        }
    }

    private void displayGameMessage(PairInterface pi, ItemComposition ic)
    {
        chatMessageManager.queue(QueuedMessage.builder()
                        .type(ChatMessageType.CONSOLE)
                        .runeLiteFormattedMessage(pi.getMessage(ic))
                        .build());
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event)
    {
        if (event.getWidget() != null
                && config.checkWithExamine()
                && event.getMenuOption().equals(EXAMINE))
        {
            final int widgetId = event.getWidget().getId();

            if(!acceptedWidgetIds.contains(widgetId)) return;

            assert client != null;
            ItemComposition composition = client.getItemDefinition(event.getWidget().getItemId());
            int itemId = composition.getId();

            if(payments.containsKey(itemId))
                displayGameMessage(payments.get(itemId), composition);

        }
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event)
    {
        if (event.getMenuEntry().getWidget() != null
                && !config.checkWithExamine()
                && event.getOption().equals(EXAMINE))
        {
            if (!acceptedWidgetIds.contains(event.getMenuEntry().getWidget().getId()))
                return;

            int itemId = event.getMenuEntry().getWidget().getItemId();

            if (payments.containsKey(itemId))
            {
                assert client != null;
                ItemComposition ic = client.getItemDefinition(itemId);

                client.getMenu().createMenuEntry(-1)
                        .setOption(CHECK_PAYMENT)
                        .setTarget(event.getTarget())
                        .setType(MenuAction.RUNELITE)
                        .onClick(e ->
                        {
                            displayGameMessage(payments.get(itemId), ic);
                        });
            }

        }
    }

    public interface PairInterface {

        int[] getTargetItems();

        String getMessage(ItemComposition ic);
    }

    static class PaymentPair implements PairInterface {
        String payment;
        int[] pairedIDs;
        String preferredName = null;

        public PaymentPair(String payment, int[] pairedIDs) {
            this.payment = payment;
            this.pairedIDs = pairedIDs;
        }

        public PaymentPair(String payment, int[] pairedIDs, String preferredName) {
            this(payment, pairedIDs);
            this.preferredName = preferredName;
        }

        @Override
        public int[] getTargetItems()
        {
            return this.pairedIDs;
        }

        public String getMessage(ItemComposition ic) {
            String text = ic.getName();
            if (getPreferredName() != null)
                text = getPreferredName();
            return new ChatMessageBuilder()
                    .append(ChatColorType.NORMAL)
                    .append("A farmer will watch over ")
                    .append(grammatify(text))
                    .append(" ")
                    .append(ChatColorType.HIGHLIGHT)
                    .append(stripAndShrink(text))
                    .append(ChatColorType.NORMAL)
                    .append(" patch for ")
                    .append(ChatColorType.HIGHLIGHT)
                    .append(this.payment)
                    .build();
        }

        public String getPreferredName() {
            return preferredName;
        }
    }

    private static class CustomPair implements PairInterface {

        String message = "";
        int[] pairedIDs;
        String preferredName = null;

        public CustomPair(int[] pairedIDs) {
            this.pairedIDs = pairedIDs;
        }

        public CustomPair(String message, int[] pairedIDs) {
            this.message = " " + message;
            this.pairedIDs = pairedIDs;
        }

        @Override
        public int[] getTargetItems()
        {
            return this.pairedIDs;
        }

        public String getMessage(ItemComposition ic) {
            String text = ic.getName();
            if (getPreferredName() != null)
                text = getPreferredName();
            return new ChatMessageBuilder()
                    .append(ChatColorType.NORMAL)
                    .append(grammatify(text).replace('a', 'A'))
                    .append(" ")
                    .append(ChatColorType.HIGHLIGHT)
                    .append(stripAndShrink(text))
                    .append(ChatColorType.NORMAL)
                    .append(" patch can ")
                    .append(ChatColorType.HIGHLIGHT)
                    .append("NOT")
                    .append(ChatColorType.NORMAL)
                    .append(" be protected by a farmer")
                    .append(this.message)
                    .build();
        }

        public String getPreferredName() {
            return preferredName;
        }
    }

    private static String stripAndShrink(String text) {
        return text
                .replace("seed", "")
                .replace("sapling", "")
                .replace("spore", "")
                .toLowerCase().trim();
    }

    private static String grammatify(String text) {
        char[] vowels = {'a', 'e', 'i', 'o', 'u'};
        for (char vowel : vowels)
            if (text.toLowerCase().charAt(0) == vowel)
                return "an";
        return "a";
    }
}
