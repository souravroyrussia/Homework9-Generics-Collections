import java.util.*;

public class Main {
    public static void main(String[] args) {
        runPortfolioExercise();
        runWarehouseExercise();
    }

    private static void runPortfolioExercise() {
        System.out.println("\n========== EXERCISE 1: INVESTOR PORTFOLIO ==========");
        Portfolio portfolio = new Portfolio();

        portfolio.addRequest(new Position(new Stock("AAPL", 190, 5), 10));
        portfolio.addRequest(new Position(new Bond("GOV2028", 1000, 4.5), 2));
        portfolio.addRequest(new Position(new Fund("TECH", 250,
                Arrays.asList("AAPL", "MSFT", "NVDA")), 3));
        portfolio.addRequest(new Position(new Stock("AAPL", 190, 5), 5));

        System.out.println("Purchase requests added to queue.");
        Position executed;
        while ((executed = portfolio.executeRequest()) != null) {
            System.out.println("Executed: " + executed);
        }

        System.out.println("Portfolio total value: " + portfolio.getPortfolioSum());
        System.out.println("Securities grouped by type:");
        for (Map.Entry<Instrument.Type, List<Position>> entry
                : portfolio.getInstrumentsByType().entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
        System.out.println("Types in portfolio: " + portfolio.getInstrumentTypes());
    }

    private static void runWarehouseExercise() {
        System.out.println("\n========== EXERCISE 2: TWO WAREHOUSES ==========");

        SackWarehouse sacks = new SackWarehouse();
        Sack sand1 = new Sack(Sack.Type.SAND, 25);
        Sack cement = new Sack(Sack.Type.CEMENT, 50);
        Sack plaster = new Sack(Sack.Type.PLASTER, 20);
        Sack sand2 = new Sack(Sack.Type.SAND, 30);
        sacks.addItem(sand1);
        sacks.addItem(cement);
        sacks.addItem(plaster);
        sacks.addItem(sand2);

        System.out.println("\nInitial sack stock:");
        sacks.getItems().forEach(System.out::println);
        sacks.addOrder(sand1);
        sacks.addVIPOrder(cement);
        System.out.println("Regular order added: " + sand1);
        System.out.println("VIP order added: " + cement);
        System.out.println("First processed sack (VIP first): " + sacks.processOrder());
        System.out.println("Remaining sacks grouped by type:");
        for (Map.Entry<Sack.Type, List<Sack>> entry : sacks.getSacksByTypes().entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
        System.out.println("Empty sack types: " + sacks.getEmptyTypes());

        BoxWarehouse boxes = new BoxWarehouse();
        Box fragile = new Box(30, 20, 15, true);
        Box normal = new Box(40, 25, 20, false);
        Box fragile2 = new Box(10, 10, 10, true);
        boxes.addItem(fragile);
        boxes.addItem(normal);
        boxes.addItem(fragile2);

        System.out.println("\nFragile boxes before orders: " + boxes.getFragileAmount());
        boxes.addVIPOrder(fragile);
        boxes.addOrder(normal);
        System.out.println("VIP order added (fragile): " + fragile);
        System.out.println("Regular order added (non-fragile): " + normal);
        System.out.println("First processed box (VIP first): " + boxes.processOrder());
        System.out.println("Fragile boxes after processing: " + boxes.getFragileAmount());
        System.out.println("Boxes sorted by largest dimension:");
        boxes.getSortedByLength().forEach(System.out::println);
    }
}

abstract class Instrument {
    enum Type { STOCK, BOND, FUND }
    private final String ticker;
    private final int price;
    private final Type type;

    protected Instrument(String ticker, int price, Type type) {
        this.ticker = ticker;
        this.price = price;
        this.type = type;
    }
    public String getTicker() { return ticker; }
    public int getPrice() { return price; }
    public Type getType() { return type; }
    @Override public String toString() { return ticker + " (price=" + price + ")"; }
}

class Stock extends Instrument {
    private final int dividends;
    public Stock(String ticker, int price, int dividends) {
        super(ticker, price, Type.STOCK);
        this.dividends = dividends;
    }
    public int getDividends() { return dividends; }
}

class Bond extends Instrument {
    private final double annualYield;
    public Bond(String ticker, int price, double annualYield) {
        super(ticker, price, Type.BOND);
        this.annualYield = annualYield;
    }
    public double getAnnualYield() { return annualYield; }
}

class Fund extends Instrument {
    private final List<String> includedTickers;
    public Fund(String ticker, int price, List<String> includedTickers) {
        super(ticker, price, Type.FUND);
        this.includedTickers = new ArrayList<>(includedTickers);
    }
    public List<String> getIncludedTickers() {
        return Collections.unmodifiableList(includedTickers);
    }
}

class Position {
    private final Instrument instrument;
    private int quantity;
    public Position(Instrument instrument, int quantity) {
        if (instrument == null || quantity <= 0) {
            throw new IllegalArgumentException("Instrument must exist and quantity must be positive.");
        }
        this.instrument = instrument;
        this.quantity = quantity;
    }
    public Instrument getInstrument() { return instrument; }
    public int getQuantity() { return quantity; }
    public void addQuantity(int amount) { quantity += amount; }
    @Override public String toString() {
        return instrument.getTicker() + " x " + quantity + " (" + instrument.getType() + ")";
    }
}

class Portfolio {
    private final Queue<Position> requests = new ArrayDeque<>();
    private final Map<String, Position> positions = new LinkedHashMap<>();

    public void addRequest(Position tradeRequest) { requests.add(tradeRequest); }

    public Position executeRequest() {
        Position request = requests.poll();
        if (request == null) return null;
        String ticker = request.getInstrument().getTicker();
        Position current = positions.get(ticker);
        if (current != null) {
            current.addQuantity(request.getQuantity());
            return current;
        }
        positions.put(ticker, request);
        return request;
    }

    public int getPortfolioSum() {
        int sum = 0;
        for (Position position : positions.values()) {
            sum += position.getInstrument().getPrice() * position.getQuantity();
        }
        return sum;
    }

    public Map<Instrument.Type, List<Position>> getInstrumentsByType() {
        Map<Instrument.Type, List<Position>> grouped = new EnumMap<>(Instrument.Type.class);
        for (Position position : positions.values()) {
            Instrument.Type type = position.getInstrument().getType();
            grouped.computeIfAbsent(type, key -> new ArrayList<>()).add(position);
        }
        return grouped;
    }

    public Set<Instrument.Type> getInstrumentTypes() {
        return getInstrumentsByType().keySet();
    }
}

class Sack {
    enum Type { SAND, CEMENT, PLASTER }
    private final Type type;
    private final int weight;
    public Sack(Type type, int weight) { this.type = type; this.weight = weight; }
    public Type getType() { return type; }
    public int getWeight() { return weight; }
    @Override public String toString() { return type + " sack (" + weight + " kg)"; }
}

class Box {
    private final int length;
    private final int width;
    private final int height;
    private final boolean fragile;
    public Box(int length, int width, int height, boolean fragile) {
        this.length = length;
        this.width = width;
        this.height = height;
        this.fragile = fragile;
    }
    public int getLargestDimension() { return Math.max(length, Math.max(width, height)); }
    public boolean isFragile() { return fragile; }
    @Override public String toString() {
        return "Box(" + length + "x" + width + "x" + height + ", fragile=" + fragile + ")";
    }
}

class Warehouse<T> {
    private final List<T> items = new ArrayList<>();
    private final Deque<T> orders = new ArrayDeque<>();
    public void addItem(T item) { items.add(item); }
    public List<T> getItems() { return new ArrayList<>(items); }
    public void addOrder(T order) { orders.addLast(order); }
    public void addVIPOrder(T order) { orders.addFirst(order); }
    public T processOrder() {
        T order = orders.pollFirst();
        if (order == null) return null;
        if (!items.remove(order)) {
            System.out.println("Warning: ordered item is not in stock: " + order);
            return null;
        }
        return order;
    }
}

class SackWarehouse extends Warehouse<Sack> {
    public Map<Sack.Type, List<Sack>> getSacksByTypes() {
        Map<Sack.Type, List<Sack>> grouped = new EnumMap<>(Sack.Type.class);
        for (Sack.Type type : Sack.Type.values()) grouped.put(type, new ArrayList<>());
        for (Sack sack : getItems()) grouped.get(sack.getType()).add(sack);
        return grouped;
    }
    public Set<Sack.Type> getEmptyTypes() {
        Set<Sack.Type> empty = EnumSet.noneOf(Sack.Type.class);
        for (Map.Entry<Sack.Type, List<Sack>> entry : getSacksByTypes().entrySet()) {
            if (entry.getValue().isEmpty()) empty.add(entry.getKey());
        }
        return empty;
    }
}

class BoxWarehouse extends Warehouse<Box> {
    public List<Box> getSortedByLength() {
        List<Box> sorted = getItems();
        sorted.sort(Comparator.comparingInt(Box::getLargestDimension));
        return sorted;
    }
    public int getFragileAmount() {
        int count = 0;
        for (Box box : getItems()) if (box.isFragile()) count++;
        return count;
    }
}
