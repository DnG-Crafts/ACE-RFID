package dngsoftware.acerfid;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.io.File;

@Database(entities = {Filament.class}, version = 1, exportSchema = false)
public abstract class filamentDB extends RoomDatabase {
    public abstract MatDB matDB();
    public static volatile filamentDB INSTANCE;

    public static filamentDB getInstance(Context context) {
        closeInstance();
        synchronized (filamentDB.class) {
            INSTANCE = Room.databaseBuilder(context.getApplicationContext(), filamentDB.class, "filament_database")
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build();

        }
        return INSTANCE;
    }

    public static File getDatabaseFile(Context context) {
        return context.getDatabasePath("filament_database");
    }

    public static void closeInstance() {
        if (INSTANCE != null && INSTANCE.isOpen()) {
            INSTANCE.close();
            INSTANCE = null;
        }
    }


}