package com.sayi.rompecabezas;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.content.Intent;
import android.content.res.AssetManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;

public class ElegirPuzzle extends AppCompatActivity implements View.OnClickListener {
    static int acumulado=0;
    static String datos;
    Intent i;
    MediaPlayer mp = null;
    static boolean reproduciendo=false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.elegir_puzzle);

        Button sonidoOn=(Button)findViewById(R.id.sonidoOn);
        Button sonidoOff=(Button)findViewById(R.id.sonidoOff);
        Button salir=(Button)findViewById(R.id.salir);
        sonidoOn.setOnClickListener(this);
        sonidoOff.setOnClickListener(this);
        salir.setOnClickListener(this);

        TextView nombre= (TextView)findViewById(R.id.elige);
        EditText score=(EditText)findViewById(R.id.score);
        Bundle parametros = this.getIntent().getExtras();
        datos = parametros.getString("nombrejugador");
        if(datos!=null){
            nombre.setText("Hola, "+ datos + "! Elige un Rompecabezas!" );
        }else{
            nombre.setText("Hola de nuevo! Elige otro Rompecabezas!" ) ;
        }
        Bundle puntaje = this.getIntent().getExtras();
        if(puntaje!=null){
            int puntos=puntaje.getInt("puntaje",0);
            acumulado=acumulado+puntos;
            score.setText("Puntaje: "+ acumulado);
        }
        else{
            score.setText("Puntaje: "+ 0);
        }

        AssetManager am;
        am = getAssets();
        try {
            final String[] files  = am.list("img"); //todas la imagenes para armar

            GridView grid = findViewById(R.id.grid);
            grid.setAdapter(new AdaptadorImagen( this)); //adapta las imagenes para mostrarla en la grilla
            grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    Intent intent = new Intent(getApplicationContext(), PuzzleActivity.class);//envia al juego la imagen seleccionada
                                                                                            //por el jugador
                    intent.putExtra("assetName", files[i % files.length]);
                    startActivity(intent);
                }
            });
        } catch (IOException e) {
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT);
        }
    }


    @Override
    public void onClick(View view) {

        switch (view.getId()){

            case R.id.sonidoOn: //comienza la reproduccion
                if(!reproduciendo) {//verifica que ya no se este reproduciendo
                    mp = MediaPlayer.create(this, R.raw.jazzopedie);
                    mp.setLooping(true);
                    mp.start();
                    reproduciendo=true;
                }
                break;
            case R.id.sonidoOff:    //detiene la reproduccion
                if(reproduciendo) {
                    mp.stop();
                    mp.setLooping(false);
                    reproduciendo=false;
                }
                break;
            case R.id.salir: //sale de la aplicacion
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("EXIT", true);
                startActivity(intent);
                break;
        }

    }
}

