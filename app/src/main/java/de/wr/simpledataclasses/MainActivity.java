package de.wr.simpledataclasses;

import android.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import de.wr.simpledataclasses.databinding.ActivityMainBinding;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;

public class MainActivity extends AppCompatActivity {
    private Disposable disposable;

//    @Inject
//    Sample sampleField;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
//
////        DataObject1$Builder
////        Toast.makeText(this, sampleField.getValue(), Toast.LENGTH_LONG ).show();
//
//        de.wr.simpledataclasses.DataObject2 build = de.wr.simpledataclasses.DataObject2.builder().list(
//                Collections.singletonList(
//                        Collections.singletonList(DataObject1.builder().build())))
//                .build();
//
//        DataObject3 o3 = DataObject3.builder().number(null).build();
//        Number number = o3.number();
//        if (number != null) {
//            number.byteValue();
//        }
//
//        Gson gson = new GsonBuilder().registerTypeAdapterFactory(DataFactoryTypeAdapterFactory.create()).create();
//        String json = gson.toJson(o3);
//        DataObject3 o3New = gson.fromJson(json, DataObject3.class);
//        System.out.println(o3.equals(o3New));
//
//        SimpleObject simpleObject = SimpleObject.builder().value1(2).value2("String").value3(Collections.emptyList()).build();
//        int i = simpleObject.value1();
//
//        String json2 = "{" +
//                "\"value_1\":24," +
//                "\"value_2\":\"This is a test\"," +
//                "\"value_3\": [" +
//                "    \"test 1\"," +
//                "    \"test 2\"," +
//                "    \"test 3\"" +
//                "]" +
//                "}";
//        System.out.println(json2);
//        SimpleObjectNamed simpleObjectNamed = gson.fromJson(json2, SimpleObjectNamed.class);
//        System.out.println(simpleObjectNamed.toString());
//        System.out.println(gson.toJson(simpleObjectNamed));
    }

    @Override
    protected void onResume() {
        super.onResume();
        dispose();
        disposable = Maybe.just("test")
                .flatMap(s -> Single.just("hallo " + s)
                        .delay(2, TimeUnit.SECONDS)
                        .toMaybe()
                        )
                .filter(x -> x.contains("t"))
                .observeOn(mainThread())
                .subscribe(
                    success -> {
                        Toast.makeText(this, "Success:" + success, Toast.LENGTH_LONG).show();
                    }, error -> {
                        Toast.makeText(this, "Error:" + error, Toast.LENGTH_LONG).show();
                    }
                 );
    }

    @Override
    protected void onPause() {
        super.onPause();
        dispose();
    }

    private void dispose() {
        if (disposable != null) {
            disposable.dispose();
        }
    }
}
