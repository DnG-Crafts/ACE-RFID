package dngsoftware.acerfid;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class tagAdapter extends RecyclerView.Adapter<tagAdapter.ViewHolder> {

    private final tagItem[] tagItems;
    private final Context context;

    public tagAdapter(Context context, tagItem[] items) {
        this.context = context;
        tagItems = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_tag, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        tagItem currentItem = tagItems[position];
        if (currentItem != null) {
            holder.itemKey.setText(currentItem.tKey);
            holder.itemValue.setText(currentItem.tValue);
            holder.itemImage.setImageDrawable(currentItem.tImage);
        }
    }

    @Override
    public int getItemCount() {
        return tagItems.length;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView itemKey;
        TextView itemValue;
        ImageView itemImage;
        ViewHolder(View itemView) {
            super(itemView);
            itemKey = itemView.findViewById(R.id.itemKey);
            itemValue = itemView.findViewById(R.id.itemValue);
            itemImage = itemView.findViewById(R.id.itemImage);
        }
    }
}