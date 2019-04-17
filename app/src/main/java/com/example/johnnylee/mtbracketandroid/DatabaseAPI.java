package com.example.johnnylee.mtbracketandroid;

import android.util.Log;

import com.amazonaws.amplify.generated.graphql.ListRacersQuery;
import com.amazonaws.amplify.generated.graphql.UpdateRacerMutation;
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient;
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;

import java.util.List;

import javax.annotation.Nonnull;

import type.UpdateRacerInput;

public class DatabaseAPI {

    private static AWSAppSyncClient mAWSAppSyncClient;
    private List<ListRacersQuery.Item> racers = null;

    public DatabaseAPI(AWSAppSyncClient mAWSAppSyncClient) {
        DatabaseAPI.mAWSAppSyncClient = mAWSAppSyncClient;
        runQuery();
    }

    public List<ListRacersQuery.Item> getRacers() {
        return racers;
    }

    //AWS Queries and Mutations
    public static void runMutation(String id, String qualificationTime){
        UpdateRacerInput updateRacerInput = UpdateRacerInput.builder().
                id(id).
                qualificationTime(qualificationTime).
                build();

        mAWSAppSyncClient.mutate(UpdateRacerMutation.builder().input(updateRacerInput).build())
                .enqueue(mutationCallback);
    }

    private static GraphQLCall.Callback<UpdateRacerMutation.Data> mutationCallback = new GraphQLCall.Callback<UpdateRacerMutation.Data>() {
        @Override
        public void onResponse(@Nonnull Response<UpdateRacerMutation.Data> response) {
        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
        }
    };

    public void runQuery(){
        mAWSAppSyncClient.query(ListRacersQuery.builder().build())
                .responseFetcher(AppSyncResponseFetchers.CACHE_AND_NETWORK)
                .enqueue(racersCallback);
    }

    private GraphQLCall.Callback<ListRacersQuery.Data> racersCallback = new GraphQLCall.Callback<ListRacersQuery.Data>() {
        @Override
        public void onResponse(@Nonnull Response<ListRacersQuery.Data> response) {
            racers = response.data().listRacers().items();
        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
        }
    };
}
