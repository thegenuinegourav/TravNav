package com.example.travnav;

import android.content.Context;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.example.travnav.utils.Constant;

import java.util.List;

import static com.example.travnav.utils.Constant.DESTINATION_COUNT_HEIGHT_BANDWIDTH;
import static com.example.travnav.utils.Constant.DESTINATION_LIST_HEIGHT;

public class DestinationAdapter extends RecyclerView.Adapter<DestinationAdapter.ViewHolder> {
    private List<Integer> mDataSet;
    private Context mContext;
    private DestinationAdapterToMapActivityCallback destinationAdapterToMapActivityCallback;
    private int listSize=0;
    private RecyclerView mRecyclerView;

    public DestinationAdapter(Context context, List<Integer> destinationsListCounts){
        mContext = context;
        mDataSet = destinationsListCounts;
        // TODO check this
        destinationAdapterToMapActivityCallback = (DestinationAdapterToMapActivityCallback) context;

    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        public EditText destinationEditText;
        public ImageView destinationCloseButton;
        public LinearLayout linearLayout;
        public ViewHolder(View v){
            super(v);
            destinationEditText = (EditText) v.findViewById(R.id.destination_edit_text);
            destinationCloseButton = (ImageView) v.findViewById(R.id.destination_close_button);
            linearLayout = (LinearLayout) v.findViewById(R.id.destination_layout);
        }
    }

    @Override
    public DestinationAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        // Create a new View
        View v = LayoutInflater.from(mContext).inflate(R.layout.custom_view,parent,false);
        listSize++;
        return new ViewHolder(v);
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mRecyclerView = recyclerView;
    }


    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position){

        holder.destinationEditText.setHint("Enter destination");

        holder.destinationEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if(actionId == EditorInfo.IME_ACTION_SEARCH
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || keyEvent.getAction() == KeyEvent.ACTION_DOWN
                        || keyEvent.getAction() == KeyEvent.KEYCODE_ENTER){

                        String destination = holder.destinationEditText.getText().toString();
                        if (destination.trim().equalsIgnoreCase("")) {
                            holder.destinationEditText.setError("Destination can not be blank");
                        }else {
                            destinationAdapterToMapActivityCallback.onDestinationEditTextClick(destination);
                        }
                        return true;
                }
                return false;
            }
        });

        // Set a click listener for item remove button
        holder.destinationCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Get the clicked item label
                Integer itemLabel = mDataSet.get(position);

                // Remove the item on remove/button click
                mDataSet.remove(position);
                holder.destinationEditText.getText().clear();
                destinationAdapterToMapActivityCallback.updateDestinationPosition();

                notifyItemRemoved(position);

                notifyItemRangeChanged(position,mDataSet.size());

                changeHeightOfRecyclerView();

                // Show the removed item label
                Toast.makeText(mContext,"Removed : " + itemLabel,Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount(){
        return mDataSet.size();
    }

    public void changeHeightOfRecyclerView() {
        ViewGroup.LayoutParams params=mRecyclerView.getLayoutParams();
        if (mDataSet.size()-1 > DESTINATION_COUNT_HEIGHT_BANDWIDTH) {
            params.height=DESTINATION_LIST_HEIGHT;
        }else {
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        }
        mRecyclerView.setLayoutParams(params);
    }
}
