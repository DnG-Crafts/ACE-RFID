package dngsoftware.acerfid;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface MatDB {

   @SuppressWarnings("UnusedDeclaration")
   @Insert
   void addItem(Filament item);

   @SuppressWarnings("UnusedDeclaration")
   @Update
   void updateItem(Filament item);

   @SuppressWarnings("UnusedDeclaration")
   @Query("UPDATE filament_table SET filament_position =:pos WHERE filament_id =:filamentID")
   void updatePosition(int pos, String filamentID);

   @SuppressWarnings("UnusedDeclaration")
   @Delete
   void deleteItem(Filament item);

   @SuppressWarnings("UnusedDeclaration")
   @Query("SELECT COUNT(dbKey) FROM filament_table")
   int getItemCount();

   @SuppressWarnings("UnusedDeclaration")
   @Query("SELECT * FROM filament_table ORDER BY filament_position ASC")
   List<Filament> getAllItems();

   @SuppressWarnings("UnusedDeclaration")
   @Query("DELETE FROM filament_table")
   void deleteAll();

   @SuppressWarnings("UnusedDeclaration")
   @Query("SELECT * FROM filament_table  WHERE filament_vendor = :filamentVendor ORDER BY filament_position ASC")
   List<Filament> getFilamentsByVendor(String filamentVendor);

   @SuppressWarnings("UnusedDeclaration")
   @Query("SELECT * FROM filament_table WHERE filament_id = :filamentID")
   Filament getFilamentById(String filamentID);

   @SuppressWarnings("UnusedDeclaration")
   @Query("SELECT * FROM filament_table WHERE filament_name = :filamentName")
   Filament getFilamentByName(String filamentName);

   @SuppressWarnings("UnusedDeclaration")
   @Query("SELECT * FROM filament_table WHERE filament_vendor || ' ' || filament_name = :descName")
   Filament getFilamentByDesc(String descName);
}