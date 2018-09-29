package com.sayi.rompecabezas;

import android.content.Intent;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;


import static java.lang.Math.abs;

public class PuzzleActivity extends AppCompatActivity {
    ArrayList<PuzzlePiece> pieces;
    String mCurrentPhotoPath;
    String mCurrentPhotoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_puzzle);

        final RelativeLayout layout = findViewById(R.id.layout);
        final ImageView imageView = findViewById(R.id.imageView);

        Intent intent = getIntent();
        final String assetName = intent.getStringExtra("assetName"); //obtiene la imagen seleccionada por el jugador

        // ejecuta esto después de que la vista fue diseñada para tener todas las dimensiones calculadas
        imageView.post(new Runnable() {
            @Override
            public void run() {
                if (assetName != null) {
                    configurar(assetName, imageView); //configura el tamaño de la imagen elegida y el marco de la vista
                }
                pieces = splitImage(); //divide la imagen en partes
                TactilListener tactilListener = new TactilListener(PuzzleActivity.this);
                // ordena de manera aleatoria las piezas
                Collections.shuffle(pieces);
                for (PuzzlePiece piece : pieces) {
                    piece.setOnTouchListener(tactilListener);
                    layout.addView(piece);
                    // randomize position, on the bottom of the screen
                    RelativeLayout.LayoutParams lParams = (RelativeLayout.LayoutParams) piece.getLayoutParams();
                    lParams.leftMargin = new Random().nextInt(layout.getWidth() - piece.pieceWidth);
                    lParams.topMargin = layout.getHeight() - piece.pieceHeight;
                    piece.setLayoutParams(lParams);
                }
            }
        });
    }

    private void configurar(String assetName, ImageView imageView) {
        // obtiene las dimensiones de la vista que sera el marco
        int targetW = imageView.getWidth();
        int targetH = imageView.getHeight();

        AssetManager am = getAssets();
        try {
            InputStream is = am.open("img/" + assetName);
            // obtiene las dimensiones del bitmap
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, new Rect(-1, -1, -1, -1), bmOptions);
            int photoW = bmOptions.outWidth;
            int photoH = bmOptions.outHeight;

            // determina cuanto se debe escalar la imagen
            int escala = Math.min(photoW/targetW, photoH/targetH);

            is.reset();

            // Decode the image file into a Bitmap sized to fill the View
            bmOptions.inJustDecodeBounds = false;
            bmOptions.inSampleSize = escala;
            bmOptions.inPurgeable = true;

            Bitmap bitmap = BitmapFactory.decodeStream(is, new Rect(-1, -1, -1, -1), bmOptions);
            imageView.setImageBitmap(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private ArrayList<PuzzlePiece> splitImage() {
        int numerodepiezas = 12; //por defecto va a dividir siempre el rompecabezas en 12 piezas
        int filas = 4;
        int columnas = 3;

        ImageView imageView = findViewById(R.id.imageView);
        ArrayList<PuzzlePiece> pieces = new ArrayList<>(numerodepiezas);

        // Obtiene el mapa de bits escalado de la imagen fuente
        BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();
        Bitmap bitmap = drawable.getBitmap();

        int[] dimensions = obtenerPosBitmapDentrodeImagenView(imageView);
        int escalaBitmapIzq = dimensions[0];
        int escalaBitmapsDer = dimensions[1];
        int escalaBitmapAncho = dimensions[2];
        int escalaBitmapAlto = dimensions[3];

        int nuevoAnchoimag = escalaBitmapAncho - 2 * abs(escalaBitmapIzq);
        int nuevoAltoimag = escalaBitmapAlto - 2 * abs(escalaBitmapsDer);

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, escalaBitmapAncho, escalaBitmapAlto, true);
        Bitmap croppedBitmap = Bitmap.createBitmap(scaledBitmap, abs(escalaBitmapIzq), abs(escalaBitmapsDer), nuevoAnchoimag, nuevoAltoimag);

        // calcula el ancho y alto de las piezas
        int anchopiezas = nuevoAnchoimag/columnas;
        int altopiezas = nuevoAltoimag/filas;

        // Crea cada pieza de mapa de bits y la agréga a la matriz resultante
        int yCoord = 0;
        for (int row = 0; row < filas; row++) {
            int xCoord = 0;
            for (int col = 0; col < columnas; col++) {
                // calcular el desplazamiento para cada pieza
                int offsetX = 0;
                int offsetY = 0;
                if (col > 0) {
                    offsetX = anchopiezas / 3;
                }
                if (row > 0) {
                    offsetY = altopiezas / 3;
                }

                // aplica el desplazamiento para cada pieza
                Bitmap pieceBitmap = Bitmap.createBitmap(croppedBitmap, xCoord - offsetX, yCoord - offsetY, anchopiezas + offsetX, altopiezas + offsetY);
                PuzzlePiece pieza = new PuzzlePiece(getApplicationContext());
                pieza.setImageBitmap(pieceBitmap);
                pieza.xCoord = xCoord - offsetX + imageView.getLeft();
                pieza.yCoord = yCoord - offsetY + imageView.getTop();
                pieza.pieceWidth = anchopiezas + offsetX;
                pieza.pieceHeight = altopiezas + offsetY;

                // este mapa de bits mantendrá nuestra la imagen final de la pieza del rompecabezas
                Bitmap piezaRompecabezas = Bitmap.createBitmap(anchopiezas + offsetX, altopiezas + offsetY, Bitmap.Config.ARGB_8888);

                //
                int bumpSize = altopiezas / 4;
                Canvas canvas = new Canvas(piezaRompecabezas);
                Path path = new Path();
                path.moveTo(offsetX, offsetY);
                if (row == 0) {
                    // lado alto de la pieza
                    path.lineTo(pieceBitmap.getWidth(), offsetY);
                } else {
                    // lado najo
                    path.lineTo(offsetX + (pieceBitmap.getWidth() - offsetX) / 3, offsetY);
                    path.cubicTo(offsetX + (pieceBitmap.getWidth() - offsetX) / 6, offsetY - bumpSize, offsetX + (pieceBitmap.getWidth() - offsetX) / 6 * 5, offsetY - bumpSize, offsetX + (pieceBitmap.getWidth() - offsetX) / 3 * 2, offsetY);
                    path.lineTo(pieceBitmap.getWidth(), offsetY);
                }

                if (col == columnas - 1) {
                    // lado derecho de la pieza
                    path.lineTo(pieceBitmap.getWidth(), pieceBitmap.getHeight());
                } else {
                    // bajoderecho
                    path.lineTo(pieceBitmap.getWidth(), offsetY + (pieceBitmap.getHeight() - offsetY) / 3);
                    path.cubicTo(pieceBitmap.getWidth() - bumpSize,offsetY + (pieceBitmap.getHeight() - offsetY) / 6, pieceBitmap.getWidth() - bumpSize, offsetY + (pieceBitmap.getHeight() - offsetY) / 6 * 5, pieceBitmap.getWidth(), offsetY + (pieceBitmap.getHeight() - offsetY) / 3 * 2);
                    path.lineTo(pieceBitmap.getWidth(), pieceBitmap.getHeight());
                }

                if (row == filas - 1) {
                    // lateral inferior de la pieza
                    path.lineTo(offsetX, pieceBitmap.getHeight());
                } else {
                    // bajo
                    path.lineTo(offsetX + (pieceBitmap.getWidth() - offsetX) / 3 * 2, pieceBitmap.getHeight());
                    path.cubicTo(offsetX + (pieceBitmap.getWidth() - offsetX) / 6 * 5,pieceBitmap.getHeight() - bumpSize, offsetX + (pieceBitmap.getWidth() - offsetX) / 6, pieceBitmap.getHeight() - bumpSize, offsetX + (pieceBitmap.getWidth() - offsetX) / 3, pieceBitmap.getHeight());
                    path.lineTo(offsetX, pieceBitmap.getHeight());
                }

                if (col == 0) {
                    // lado izquierdo
                    path.close();
                } else {
                    // bajo izquierdo
                    path.lineTo(offsetX, offsetY + (pieceBitmap.getHeight() - offsetY) / 3 * 2);
                    path.cubicTo(offsetX - bumpSize, offsetY + (pieceBitmap.getHeight() - offsetY) / 6 * 5, offsetX - bumpSize, offsetY + (pieceBitmap.getHeight() - offsetY) / 6, offsetX, offsetY + (pieceBitmap.getHeight() - offsetY) / 3);
                    path.close();
                }
                //decoracion

                // mascara de la pieza
                Paint paint = new Paint();
                paint.setColor(0XFF000000);
                paint.setStyle(Paint.Style.FILL);

                canvas.drawPath(path, paint);
                paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
                canvas.drawBitmap(pieceBitmap, 0, 0, paint);

                // dibuja el borde blanco de la pieza
                Paint border = new Paint();
                border.setColor(0X80FFFFFF);
                border.setStyle(Paint.Style.STROKE);
                border.setStrokeWidth(8.0f);
                canvas.drawPath(path, border);

                // el borde oscuro de la pieza
                border = new Paint();
                border.setColor(0X80000000);
                border.setStyle(Paint.Style.STROKE);
                border.setStrokeWidth(3.0f);
                canvas.drawPath(path, border);

                // setea el resultante bitmap a la pieza
                pieza.setImageBitmap(piezaRompecabezas);

                pieces.add(pieza);
                xCoord += anchopiezas;
            }
            yCoord += altopiezas;
        }

        return pieces;
    }

    private int[] obtenerPosBitmapDentrodeImagenView(ImageView imageView) {
        int[] ret = new int[4];

        if (imageView == null || imageView.getDrawable() == null)
            return ret;

        // obtiene las dimensiones de la imagen
        //  obtiene los valores de la matriz de la imagen y los reemplaza en el array
        float[] f = new float[9];
        imageView.getImageMatrix().getValues(f);

        // Extrae los valores de las constantes scaleX == scaleY)
        final float scaleX = f[Matrix.MSCALE_X];
        final float scaleY = f[Matrix.MSCALE_Y];

        //
        final Drawable d = imageView.getDrawable();
        final int origW = d.getIntrinsicWidth();
        final int origH = d.getIntrinsicHeight();

        // Calcula las dimensiones actuales
        final int actW = Math.round(origW * scaleX);
        final int actH = Math.round(origH * scaleY);

        ret[2] = actW;
        ret[3] = actH;

        // obtiene la posicion de la imagen
        // asume que la imagen esta centrada en la  ImageView
        int imgViewW = imageView.getWidth();
        int imgViewH = imageView.getHeight();

        int top = (int) (imgViewH - actH)/2;
        int left = (int) (imgViewW - actW)/2;

        ret[0] = left;
        ret[1] = top;

        return ret;
    }

    public void checkGameOver() { //siempre controla si el juego esta terminado o no...
        if (isGameOver()) {

            Intent intent = new Intent(getApplicationContext(), ElegirPuzzle.class);
            intent.putExtra("puntaje",10);
            startActivityForResult(intent, 0);
            //finish();
        }
    }

    private boolean isGameOver() {
        for (PuzzlePiece piece : pieces) {
            if (piece.canMove) {
                return false;
            }
        }

        return true;
    }


   }
