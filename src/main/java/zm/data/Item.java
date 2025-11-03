package zm.data;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Table;
import lombok.Setter;

import java.util.Objects;

@Setter
@Table(name = "items")
@Embeddable
public class Item {
    private String name;
    private String description;

    public Item() {
        name = "Unnamed item";
        description = "";
    }

    public Item(String name, String description) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name can't be null or empty");
        }
        if (description == null) {
            throw new IllegalArgumentException("Description can't be null");
        }
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Item item = (Item) o;
        return Objects.equals(name, item.name) && Objects.equals(description, item.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description);
    }

    @Override
    public String toString() {
        return "Item{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +                '}';
    }
}
