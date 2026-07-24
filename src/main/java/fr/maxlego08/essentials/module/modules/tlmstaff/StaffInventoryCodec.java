package fr.maxlego08.essentials.module.modules.tlmstaff;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

public final class StaffInventoryCodec {

    private StaffInventoryCodec() {
    }

    public static String encode(ItemStack[] contents) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream(); BukkitObjectOutputStream output = new BukkitObjectOutputStream(bytes)) {
            output.writeInt(contents.length);
            for (ItemStack itemStack : contents) output.writeObject(itemStack);
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public static ItemStack[] decode(String value, int expectedLength) {
        try (ByteArrayInputStream bytes = new ByteArrayInputStream(Base64.getDecoder().decode(value)); BukkitObjectInputStream input = new BukkitObjectInputStream(bytes)) {
            int length = input.readInt();
            if (length != expectedLength) throw new IllegalStateException("Unexpected inventory size: " + length);
            ItemStack[] contents = new ItemStack[length];
            for (int index = 0; index < contents.length; index++) contents[index] = (ItemStack) input.readObject();
            return contents;
        } catch (IOException | ClassNotFoundException | IllegalArgumentException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
