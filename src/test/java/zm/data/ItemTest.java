package zm.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ItemTest {

    @Test
    void testItemCreation() {
        Item item = new Item("Sofa", "Old leather sofa");
        
        assertNotNull(item);
        assertEquals("Sofa", item.getName());
        assertEquals("Old leather sofa", item.getDescription());
    }

    @Test
    void testItemCreationWithNullName() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Item(null, "Some description");
        });
    }

    @Test
    void testItemCreationWithEmptyName() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Item("", "Some description");
        });
    }

    @Test
    void testItemCreationWithBlankName() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Item("   ", "Some description");
        });
    }

    @Test
    void testItemCreationWithNullDescription() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Item("Mattress", null);
        });
    }

    @Test
    void testItemCreationWithEmptyDescription() {
        Item item = new Item("Mattress", "");
        
        assertNotNull(item);
        assertEquals("", item.getDescription());
    }

    @Test
    void testItemEquality() {
        Item item1 = new Item("Fridge", "Broken refrigerator");
        Item item2 = new Item("Fridge", "Broken refrigerator");
        
        assertEquals(item1, item2);
    }

    @Test
    void testItemInequalityDifferentName() {
        Item item1 = new Item("Fridge", "Broken refrigerator");
        Item item2 = new Item("Freezer", "Broken refrigerator");
        
        assertNotEquals(item1, item2);
    }

    @Test
    void testItemInequalityDifferentDescription() {
        Item item1 = new Item("Fridge", "Broken refrigerator");
        Item item2 = new Item("Fridge", "Old refrigerator");
        
        assertNotEquals(item1, item2);
    }

    @Test
    void testItemHashCode() {
        Item item1 = new Item("TV", "Old CRT television");
        Item item2 = new Item("TV", "Old CRT television");
        
        assertEquals(item1.hashCode(), item2.hashCode());
    }

    @Test
    void testItemToString() {
        Item item = new Item("Washing Machine", "Broken washing machine");
        
        String result = item.toString();
        
        assertNotNull(result);
        assertTrue(result.contains("Washing Machine"));
        assertTrue(result.contains("Broken washing machine"));
    }

    @Test
    void testItemWithLongDescription() {
        String longDescription = "A".repeat(1000);
        Item item = new Item("Chair", longDescription);
        
        assertEquals(longDescription, item.getDescription());
    }

    @Test
    void testItemWithSpecialCharacters() {
        Item item = new Item("Oven & Stove", "Kitchen appliance - 220V (broken)");
        
        assertEquals("Oven & Stove", item.getName());
        assertEquals("Kitchen appliance - 220V (broken)", item.getDescription());
    }
}