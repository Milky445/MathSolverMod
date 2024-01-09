package org.mathsolver.mathsolveractualfinal;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.minecraft.client.Minecraft.getInstance;


// The value here should match an entry in the META-INF/mods.toml file
@Mod(MathSolverActualFinal.MODID)
public class MathSolverActualFinal {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "mathsolver";
    public static final String MODVERSION = "BETA-1.0.3";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "mathsolver" namespace
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "mathsolver" namespace
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    // Creates a new Block with the id "mathsolver:example_block", combining the namespace and path
    public static final RegistryObject<Block> EXAMPLE_BLOCK = BLOCKS.register("example_block", () -> new Block(BlockBehaviour.Properties.of(Material.STONE)));
    // Creates a new BlockItem with the id "mathsolver:example_block", combining the namespace and path
    public static final RegistryObject<Item> EXAMPLE_BLOCK_ITEM = ITEMS.register("example_block", () -> new BlockItem(EXAMPLE_BLOCK.get(), new Item.Properties().tab(CreativeModeTab.TAB_BUILDING_BLOCKS)));

    public MathSolverActualFinal() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the commonSetup method for mod loading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(MathSolverActualFinal.class);
    }

    @Mod.EventBusSubscriber(value = Dist.CLIENT, modid = MathSolverActualFinal.MODID)
    public static class ChatEventHandler {
        @SubscribeEvent
        public static void onChatMessage(ClientChatEvent event) {
            String message = event.getMessage();
            // Prevent the message from being sent to the server
            if (isQuadraticEquation(message)) {
                event.setCanceled(true);
                handleQuadraticEquation(message);
            } else if (isArithmeticExpression(message)) {
                event.setCanceled(true);
                if (message.contains("log(")) {
                    handleLogarithmicExpression(message);
                } else {
                    handleArithmeticExpression(message);
                }
            } else if (isHelp(message)){
                event.setCanceled(true);
                handleHelpMessage(message);
            } else {
                Minecraft.getInstance().gui.getChat().addMessage(Component.nullToEmpty("Support not added for that operation. Type ?help for more info."));
                Minecraft.getInstance().gui.getChat().addMessage(Component.nullToEmpty(message));
            }
            event.setCanceled(true);
        }
    }

    static void handleQuadraticEquation(String message) {
        try {
            LOGGER.info("Message is a quadratic equation, parsing...");
            double[] coefficients = MathSolverActualFinal.parseQuadraticEquation(message);
            solveQuadratic(coefficients[0], coefficients[1], coefficients[2]);
        } catch (IllegalArgumentException e) {
            LOGGER.error("Error parsing quadratic equation: " + e.getMessage());
        }
    }

    static void handleArithmeticExpression(String message) {
        try {
            LOGGER.info("Message is an arithmetic equation, solving...");
            double result = solveMathEquation(message);
            if (String.valueOf(result).equals("Infinity")) {
                Minecraft.getInstance().gui.getChat().addMessage(Component.nullToEmpty("Result exceeds the calculation limit"));
            } else {
                Minecraft.getInstance().gui.getChat().addMessage(Component.nullToEmpty("Result: " + result));
            }
        } catch (Exception e) {
            LOGGER.error("Error solving arithmetic equation: " + e.getMessage());
        }
    }

    static void handleLogarithmicExpression(String message) {
        try {
            LOGGER.info("Message is a logarithmic expression, solving...");
            // Extract base and value from the logarithmic expression
            Pattern logPattern = Pattern.compile("log\\((\\d+),\\s*(\\d+)\\)");
            Matcher matcher = logPattern.matcher(message);

            if (matcher.find()) {
                double base = Double.parseDouble(matcher.group(1));
                double value = Double.parseDouble(matcher.group(2));

                // Compute the logarithm
                double result = Math.log(value) / Math.log(base);
                Minecraft.getInstance().gui.getChat().addMessage(Component.nullToEmpty("Logarithm result: " + result));
            } else {
                throw new IllegalArgumentException("Invalid logarithmic expression format");
            }
        } catch (Exception e) {
            LOGGER.error("Error solving logarithmic expression: " + e.getMessage());
        }
    }

    static void handleHelpMessage(String message) {
        try {
            LOGGER.info("Message is asking for help, displaying help message...");
            Minecraft.getInstance().gui.getChat().addMessage(Component.nullToEmpty("To start, type ?solve and then your quadratic equation. For example: ?solve x^2 + 3x + 2 for x"));
            Minecraft.getInstance().gui.getChat().addMessage(Component.nullToEmpty("Or, you can type ?solve and then your arithmetic expression. For example: ?solve 1 + 2 * 3 - 4 / 5"));
            Minecraft.getInstance().gui.getChat().addMessage(Component.nullToEmpty("You can also type ?solve and then your logarithmic expression. For example: ?solve log(2, 8), where 2 is the base, and 8 is the value"));
        } catch (Exception e) {
            LOGGER.error("Error displaying help message: " + e.getMessage());
        }
    }

    static boolean isArithmeticExpression(String message) {
        // Matches basic arithmetic expressions
        LOGGER.info("Checking if message is an arithmetic expression...");
        return message.matches("^\\?solve\\s+[0-9+\\-*/().\\s^log(,)]+$");
    }

    static boolean isQuadraticEquation(String message) {
        // Matches quadratic equations like "solve 1x^2 + 3x + 2"
        LOGGER.info("Checking if message is a quadratic equation...");
        return message.matches("\\?solve\\s+(-?\\d*(\\.\\d+)?)[a-zA-Z]\\^2\\s*([+-])\\s*(-?\\d*(\\.\\d+)?)[a-zA-Z]\\s*([+-])\\s*(-?\\d*(\\.\\d+)?)\\s*for\\s*[a-zA-Z]");
    }

    static boolean isHelp(String message) {
        LOGGER.info("Checking if message is asking for help...");
        return message.matches("\\?help");
    }

    public static String solveQuadratic(double a, double b, double c) {
        double discriminant = (b * b) - (4 * a * c);
        String roots;

        if (discriminant > 0) {
            double root1 = (-b + Math.sqrt(discriminant)) / (2 * a);
            double root2 = (-b - Math.sqrt(discriminant)) / (2 * a);
            roots = "Two real roots: " + root1 + " and " + root2;
            Minecraft.getInstance().gui.getChat().addMessage(Component.nullToEmpty("Roots: " + roots));
        } else if (discriminant == 0) {
            double root = -b / (2 * a);
            roots = "One real root: " + root;
            Minecraft.getInstance().gui.getChat().addMessage(Component.nullToEmpty("Roots: " + roots));
        } else {
            double realPart = -b / (2 * a);
            double imaginaryPart = Math.sqrt(-discriminant) / (2 * a);
            roots = "Complex roots: " + realPart + " + " + imaginaryPart + "i and " + realPart + " - " + imaginaryPart + "i";
            Minecraft.getInstance().gui.getChat().addMessage(Component.nullToEmpty("Roots: " + roots));
        }
        return roots;
    }

    public static double[] parseQuadraticEquation(String input) {
        // Regex pattern to match the quadratic equation format
        Pattern pattern = Pattern.compile("\\?solve\\s+(-?\\d*(\\.\\d+)?)[a-zA-Z]\\^2\\s*([+-])\\s*(-?\\d*(\\.\\d+)?)[a-zA-Z]\\s*([+-])\\s*(-?\\d*(\\.\\d+)?)\\s*for\\s*[a-zA-Z]");
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            // Debugging: Print out each group captured by the regex
            for (int i = 1; i <= matcher.groupCount(); i++) {
                LOGGER.info("Group " + i + ": " + matcher.group(i));
            }

            double a = parseCoefficient(matcher.group(1));
            double b = parseSignedCoefficient(matcher.group(3), matcher.group(4));
            double c = parseCoefficient(matcher.group(7));

            return new double[]{a, b, c};
        } else {
            throw new IllegalArgumentException("Invalid input format");
        }
    }

    private static double parseCoefficient(String value) {
        if (value == null || value.isEmpty()) {
            return 1.0; // Default coefficient value if empty
        }
        return Double.parseDouble(value);
    }

    private static double parseSignedCoefficient(String sign, String value) {
        double coefficient = parseCoefficient(value);
        return sign.equals("-") ? -coefficient : coefficient;
    }

    private static double solveMathEquation(String message) {
        // Splitting by space for simplicity. This requires the expression to have space-separated tokens.
        String noSpaces = message.replaceAll("\\s+", "");
        List<String> tokens = new ArrayList<>();
        Matcher matcher = Pattern.compile("\\d+|\\^|[+\\-*/]").matcher(noSpaces);
        while (matcher.find()) {
            tokens.add(matcher.group());
        }

        Stack<Double> numbers = new Stack<>();
        Stack<Character> operators = new Stack<>();

        for (String token : tokens) {
            if (token.matches("\\d+")) {
                // Token is a number
                numbers.push(Double.parseDouble(token));
            } else if (token.matches("[+-/*]")) {
                // Token is an operator
                while (!operators.isEmpty() && hasPrecedence(token.charAt(0), operators.peek())) {
                    numbers.push(applyOperation(operators, numbers));
                }
                operators.push(token.charAt(0));
            } else if (token.matches("\\(")) {
                // Token is a left parenthesis
                operators.push(token.charAt(0));
            } else if (token.matches("\\)")) {
                // Token is a right parenthesis
                while (!operators.isEmpty() && operators.peek() != '(') {
                    numbers.push(applyOperation(operators, numbers));
                }
                operators.pop();
            } else if (token.matches("\\^")) {
                // Token is an exponent
                while (!operators.isEmpty() && hasPrecedence(token.charAt(0), operators.peek())) {
                    numbers.push(applyOperation(operators, numbers));
                }
                operators.push(token.charAt(0));
            }
        }
        while (!operators.isEmpty()) {
            numbers.push(applyOperation(operators, numbers));
        }

        // Final result
        return numbers.pop();
    }

    private static boolean hasPrecedence(char op1, char op2) {
        if ((op1 == '*' || op1 == '/') && (op2 == '+' || op2 == '-')) {
            return false;
        }
        return true;
    }

    private static double applyOperation(Stack<Character> operators, Stack<Double> numbers) {
        if (operators.isEmpty() || numbers.size() < 2) {
            throw new IllegalStateException("Insufficient operands or operators");
        }

        char operator = operators.pop();
        double secondOperand = numbers.pop();
        double firstOperand = numbers.pop();

        switch (operator) {
            case '+': return firstOperand + secondOperand;
            case '-': return firstOperand - secondOperand;
            case '*': return firstOperand * secondOperand;
            case '/':
                if (secondOperand == 0) throw new UnsupportedOperationException("Cannot divide by zero");
                return firstOperand / secondOperand;
            case '^':
                return Math.pow(firstOperand, secondOperand);
            default:
                throw new UnsupportedOperationException("Unsupported operator: " + operator);
        }
    }


    private void commonSetup(final FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("VERSION >> {}", MODVERSION);
        LOGGER.info("HELLO FROM COMMON SETUP");
        LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
        Minecraft.getInstance().gui.getChat().addMessage(Component.nullToEmpty("Welcome, type ?help to start!"));
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", getInstance().getUser().getName());
        }
    }
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Minecraft.getInstance().gui.getChat().addMessage(Component.nullToEmpty("Welcome, type ?help to start!"));
    }
}

