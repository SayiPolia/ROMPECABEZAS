package com.sayi.rompecabezas;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;

public class AdaptadorImagen extends BaseAdapter {
    private Context mContext;
    private AssetManager am;
    private String[] files;

    public AdaptadorImagen(Context c) {
        mContext = c;
        am = mContext.getAssets();
        try {
            files  = am.list("img"); //toma las imagenes de la carpeta assets
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getCount() {
        return files.length;
    }

    public Object getItem(int position) {
        return null;
    }

    public long getItemId(int position) {
        return 0;
    }

    // crea un nuevo ImageView por cada item referenciado por el  Adapter
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            final LayoutInflater layoutInflater = LayoutInflater.from(mContext);
            convertView = layoutInflater.inflate(R.layout.grid_element, null);
        }

        final ImageView imageView = convertView.findViewById(R.id.gridImageview);
        imageView.setImageBitmap(null);
        // ejecutar el código relacionado con la imagen después de que la vista fue presentada
        imageView.post(new Runnable() {
            @Override
            public void run() {
                new AsyncTask<Void, Void, Void>() {
                    private Bitmap bitmap;
                    @Override
                    protected Void doInBackground(Void... voids) {
                        bitmap = getPicFromAsset(imageView, files[position]);
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        super.onPostExecute(aVoid);
                        imageView.setImageBitmap(bitmap);
                    }
                }.execute();
            }
        });

        return convertView;
    }

    private Bitmap getPicFromAsset(ImageView imageView, String assetName) {
        // obtiene las dimensiones de la imagen
        int targetW = imageView.getWidth();
        int targetH = imageView.getHeight();

        if(targetW == 0 || targetH == 0) {
            // si la vista no tiene nada seteado
            return null;
        }

        try {
            InputStream is = am.open("img/" + assetName);
            // obtiene las dimensiones del bitmap
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, new Rect(-1, -1, -1, -1), bmOptions);
            int photoW = bmOptions.outWidth;
            int photoH = bmOptions.outHeight;

            // determina cuando debe escalar la imagen
            int scalarImg = Math.min(photoW/targetW, photoH/targetH);

            is.reset();

            // Decodifica el archivo de imagen del tamaño de bits para llenar la vista
            bmOptions.inJustDecodeBounds = false;
            bmOptions.inSampleSize = scalarImg;
            bmOptions.inPurgeable = true;

            return BitmapFactory.decodeStream(is, new Rect(-1, -1, -1, -1), bmOptions);
        } catch (IOException e) {
            e.printStackTrace();

            return null;
        }
    }
}