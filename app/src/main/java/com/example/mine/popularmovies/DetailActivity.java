package com.example.mine.popularmovies;


import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;


import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;

import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;


import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;

import android.util.Log;
import android.widget.LinearLayout;

import android.widget.TextView;
import android.widget.Toast;


import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.like.LikeButton;
import com.like.OnLikeListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DetailActivity extends AppCompatActivity implements ListAdapter.SetOncLickListener{

    private Movie movie=null;
    private ListAdapter reviewsAdapter;
    private ListAdapter trailersAdapter;
    private LikeButton likeButton;
    private final static String BaseURL="https://image.tmdb.org/t/p/w500";
    private   List<MovieImage> images ;
    private  SimpleDraweeView moviePoster;
    private final static String SHOW_TYPE="movie";
    private static final String API_KEY = BuildConfig.APIKey;
    private final  Random rand = new Random();
    private Toast favoriteToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int orientation=getResources().getConfiguration().orientation;

        if(orientation== Configuration.ORIENTATION_LANDSCAPE)
            setContentView(R.layout.activity_detail_landscap);
        else
            setContentView(R.layout.activity_detail);

        likeButton=(LikeButton)findViewById(R.id.favorite_button);

        movie=new Movie();
        Intent intent=getIntent();
        if(intent!=null){
            Bundle bundle=intent.getExtras();
            if(bundle!=null){

                movie=bundle.getParcelable("Movie");
                likeButton.setLiked(movie.isFavorite());

            }
        }



        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
        float dpHeight = displayMetrics.heightPixels / displayMetrics.density;

        moviePoster = (SimpleDraweeView) findViewById(R.id.movieDraweeView);


        likeButton.setOnLikeListener(new OnLikeListener(){

            @Override
            public void liked(LikeButton likeButton) {

                movie.setFavorite(true);
                getContentResolver().insert(DatabaseContract.Movies.MOVIE_URI
                        ,DatabaseContract.makeMovieContentValues(movie));
                if(favoriteToast!=null)
                    favoriteToast.cancel();
                favoriteToast=Toast.makeText(getApplicationContext(),"Movie is marked as favorite :)",Toast.LENGTH_LONG);
                favoriteToast.show();

            }

            @Override
            public void unLiked(LikeButton likeButton) {

                movie.setFavorite(false);
                String[] args=new String [1];
                args[0]=movie.getId();
                getContentResolver().delete(DatabaseContract.Movies.MOVIE_URI
                        ,DatabaseContract.Movies.ID+" =?",args);
                if(favoriteToast!=null)
                    favoriteToast.cancel();
                favoriteToast=Toast.makeText(getApplicationContext(),"Movie is removed from favorite movies :(",Toast.LENGTH_LONG);
                favoriteToast.show();



            }
        });
        TextView movieRate = (TextView) findViewById(R.id.ratingBar);
        movieRate.setText(Double.toString(movie.getVote_average()));

        TextView movieOriginalTitle = (TextView) findViewById(R.id.movieOriginalTitle);
        movieOriginalTitle.setText(movie.getOriginal_title());

        TextView movieOriginalLanguage = (TextView) findViewById(R.id.movieOriginalLanguage);
        movieOriginalLanguage.setText(getString(R.string.language)+movie.getOriginal_language());

        TextView movieOverview = (TextView) findViewById(R.id.movieOverview);
        movieOverview.setText(movie.getOverview());

        TextView movieReleaseDate = (TextView) findViewById(R.id.movieReleaseDate);
        movieReleaseDate.setText(getString(R.string.release_date)+movie.getRelease_date());

        TextView adult =(TextView) findViewById(R.id.movieType);
        adult.setText((movie.isAdult()?getString(R.string.adult):getString(R.string.family)));

        TextView voteCount = (TextView) findViewById(R.id.movieVoteCount);
        voteCount.setText(getString(R.string.vote_count)+movie.getVote_count());

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)moviePoster.getLayoutParams();





        params.width = LinearLayout.LayoutParams.MATCH_PARENT;
        params.height =(int)(dpHeight*0.55);

        if(orientation== Configuration.ORIENTATION_LANDSCAPE)
        {

            params.width = (int)(dpWidth*0.35);
            params.height =LinearLayout.LayoutParams.MATCH_PARENT;
        }


        moviePoster.setLayoutParams(params);


        RecyclerView trailersList=(RecyclerView) findViewById(R.id.trailers_list);
        RecyclerView reviewsList=(RecyclerView) findViewById(R.id.reviews_list);

        LinearLayoutManager trailersLayoutManager = new LinearLayoutManager(this);
        LinearLayoutManager reviewsLayoutManager = new LinearLayoutManager(this);

        trailersList.setLayoutManager(trailersLayoutManager);
        trailersList.setHasFixedSize(true);

        reviewsList.setLayoutManager(reviewsLayoutManager);
        reviewsList.setHasFixedSize(true);

        trailersAdapter=new ListAdapter("trailer");
        trailersAdapter.setTrailerItemOncLickListener(this);

        reviewsAdapter=new ListAdapter("review");

        trailersList.setAdapter(trailersAdapter);
        reviewsList.setAdapter(reviewsAdapter);

        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo=connectivityManager.getActiveNetworkInfo();
        if(networkInfo!=null&&networkInfo.isConnected()) {
            fetchReviews();
            fetchTrailers();
            images= new ArrayList<>();
            fetchImages(images);
        }



    }



    private void fetchImages(final List<MovieImage> images){

        ApiInterface apiService =
                APIClient.getClient().create(ApiInterface.class);

        final String TAG=this.getClass().getSimpleName();
        Call<ImagesResponse> call =
                apiService.getMovieDetails(SHOW_TYPE,
                        movie.getId()
                        , API_KEY
                        ,movie.getOriginal_language()
                        ,"en,null");
        call.enqueue(new Callback<ImagesResponse>() {
            @Override
            public void onResponse(@NonNull Call<ImagesResponse>call, @NonNull Response<ImagesResponse> response) {


                images.addAll(response.body().getBackdrops());
                images.addAll(response.body().getPosters());
                final int imagesNumber=images.size();
                startTimer(imagesNumber);


            }

            @Override
            public void onFailure(@NonNull Call<ImagesResponse>call, @NonNull Throwable t) {
                // Log error here since request failed
                Log.e(TAG, t.toString());
            }
        });




    }


    private void startTimer(final int imagesNumber){


        changePoster(imagesNumber);

        new CountDownTimer(5000,1000) {

            @Override
            public void onTick(long millisUntilFinished) {}

            @Override
            public void onFinish() {

                changePoster(imagesNumber);
                start();
            }
        }.start();
    }



    private void changePoster(int imagesNumber){



        int index=rand.nextInt(imagesNumber);
        Uri uri=Uri.parse(BaseURL+images.get(index ).getFile_path());

        ImageRequest request = ImageRequestBuilder.newBuilderWithSource(uri)
                .setResizeOptions(new ResizeOptions(images.get(index ).getWidth(),images.get(index ).getHeight()))
                .build();
        moviePoster.setController(
                Fresco.newDraweeControllerBuilder()
                        .setOldController(moviePoster.getController())
                        .setImageRequest(request)
                        .build());
    }


    private void fetchTrailers(){

        ApiInterface apiService =
                APIClient.getClient().create(ApiInterface.class);

        final String TAG=this.getClass().getSimpleName();

        Call<TrailerResponse> call =
                apiService.getMovieVideos(SHOW_TYPE,
                        movie.getId()
                        , API_KEY);
        call.enqueue(new Callback<TrailerResponse>() {
            @Override
            public void onResponse(@NonNull Call<TrailerResponse>call, @NonNull Response<TrailerResponse> response) {


                trailersAdapter.setTrailers(response.body().getResults());



            }

            @Override
            public void onFailure(@NonNull Call<TrailerResponse>call, @NonNull Throwable t) {
                // Log error here since request failed
                 Log.e(TAG, t.toString());
            }
        });

    }
    private void fetchReviews(){

        final String TAG=this.getClass().getSimpleName();
        final ApiInterface apiService =
                APIClient.getClient().create(ApiInterface.class);


        Call<ReviewsResponse> call =
                apiService.getMovieReviews(SHOW_TYPE,
                        movie.getId()
                        , API_KEY);
        call.enqueue(new Callback<ReviewsResponse>() {
            @Override
            public void onResponse(@NonNull Call<ReviewsResponse>call, @NonNull Response<ReviewsResponse> response) {


                reviewsAdapter.setReviews(response.body().getResults());
            }

            @Override
            public void onFailure(@NonNull Call<ReviewsResponse>call, @NonNull Throwable t) {
                // Log error here since request failed
                Log.e(TAG, t.toString());
            }
        });

    }

    @Override
    public void SetOnclick(Trailer trailer) {


            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("http://www.youtube.com/watch?v=" + trailer.getKey()));
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            }

    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        likeButton.setLiked(savedInstanceState.getBoolean("like"));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("like",likeButton.isLiked());
        super.onSaveInstanceState(outState);

    }
}
