public class TestLayout {
    public static void main(String[] args) {
        String[] rooms = {"P301", "P302", "P303", "P304", "101", "102A"};
        for (String r : rooms) {
            String numStr = r.replaceAll("[^0-9]", "");
            int num = 0;
            if (!numStr.isEmpty()) {
                num = Integer.parseInt(numStr);
            }
            System.out.println(r + " -> " + num + " -> " + (num % 2 != 0 ? "Odd" : "Even"));
        }
    }
}