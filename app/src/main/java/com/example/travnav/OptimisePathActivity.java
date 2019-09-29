package com.example.travnav;

import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class OptimisePathActivity extends AppCompatActivity {

    private static final String TAG = "OptimisePathActivity";
    private ArrayList<Location> locations;
    private int size;
    private float[][] graph;
    private int[] optimisePath, vertex;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_optimise_path);
        init();
    }

    public void init() {
        locations = this.getIntent().getParcelableArrayListExtra("LOCATIONS");
        Log.d(TAG, "LOCATIONS : " + locations.toString());
        size = locations.size();
        buildGraph();
        float dist = findOptimisedPath(0);
        Log.d(TAG, "Optimised Dist : " + dist + " Path : " + optimisePath.toString());
        TextView textView = (TextView) findViewById(R.id.textVw);
        textView.setText(optimisePath[0] + " "+ optimisePath[1] + " " + optimisePath[2]);
    }

    public void buildGraph() {
        graph = new float[size][size];
        for (int i=0; i<size; i++) {
            for (int j=0; j<size; j++) {
                float dist = locations.get(i).distanceTo(locations.get(j));
                graph[i][j]=dist;
            }
        }
        Log.d(TAG, "Graph : " + graph.toString());
    }

    float findOptimisedPath(int s)
    {
        int vertexSize = size-1;
        // store all vertex apart from source vertex
        vertex = new int[vertexSize];
        optimisePath = new int[vertexSize];
        for(int i=1;i<size;i++) {
            vertex[i-1]=i;
        }

        // store minimum weight Hamiltonian Cycle.
        float min_path = Float.MAX_VALUE;
        do {

            Log.d(TAG, "Vertex Array : " + vertex.toString());
            // store current Path weight(cost)
            float current_pathweight = 0;

            // compute current path weight
            int k = s;
            for (int i = 0; i < vertexSize; i++) {
                current_pathweight += graph[k][vertex[i]];
                k = vertex[i];
            }

            // update minimum
            if (min_path > current_pathweight) {
                min_path = current_pathweight;
                for (int i=0; i<vertexSize; i++)
                    optimisePath[i] = vertex[i];
            }


        } while (findNextPermutation());

        return min_path;
    }



    // Function to swap the data
    // present in the left and right indices
    public int[] swap(int data[], int left, int right) {

        // Swap the data
        int temp = data[left];
        data[left] = data[right];
        data[right] = temp;

        // Return the updated array
        return data;
    }

    // Function to reverse the sub-array
    // starting from left to the right
    // both inclusive
    public int[] reverse(int data[], int left, int right) {

        // Reverse the sub-array
        while (left < right) {
            int temp = data[left];
            data[left++] = data[right];
            data[right--] = temp;
        }

        // Return the updated array
        return data;
    }

    // Function to find the next permutation
    // of the given integer array
    public boolean findNextPermutation() {

        // If the given vertexset is empty
        // or contains only one element
        // next_permutation is not possible
        if (vertex.length <= 1)
            return false;

        int last = vertex.length - 2;

        // find the longest non-increasing suffix
        // and find the pivot
        while (last >= 0) {
            if (vertex[last] < vertex[last + 1]) {
                break;
            }
            last--;
        }

        // If there is no increasing pair
        // there is no higher order permutation
        if (last < 0)
            return false;

        int nextGreater = vertex.length - 1;

        // Find the rightmost successor to the pivot
        for (int i = vertex.length - 1; i > last; i--) {
            if (vertex[i] > vertex[last]) {
                nextGreater = i;
                break;
            }
        }

        // Swap the successor and the pivot
        vertex = swap(vertex, nextGreater, last);

        // Reverse the suffix
        vertex = reverse(vertex, last + 1, vertex.length - 1);

        // Return true as the next_permutation is done
        return true;
    }
}
