package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.widget.Toast;
import android.util.AtomicFile;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

final class ThemeSelector {
    private static String read(InputStream in) throws IOException {
        try (InputStream src=in; ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            byte[] buf=new byte[4096]; int n;
            while((n=src.read(buf))!=-1) out.write(buf,0,n);
            return new String(out.toByteArray(),StandardCharsets.UTF_8);
        }
    }
    static void show(Activity a,Runnable done) {
        try {
            TreeMap<String,JSONObject> themes=new TreeMap<>();
            for(int i=1;i<=7;i++){
                JSONObject group=new JSONObject(read(a.getAssets().open("theme-presets/group0"+i+".json")));
                Iterator<String> it=group.keys();
                while(it.hasNext()){String k=it.next();themes.put(k,group.getJSONObject(k));}
            }
            themes.put("Eclipse personalizado",new JSONObject());
            String[] names=themes.keySet().toArray(new String[0]);
            new AlertDialog.Builder(a).setTitle("Temas / Themes").setItems(names,(d,i)->
                new AlertDialog.Builder(a).setTitle(names[i])
                .setMessage("Aplicar estilo visual conservando aplicaciones y fondo actual.")
                .setPositiveButton("Aplicar",(x,y)->select(a,names[i],themes.get(names[i]),done))
                .setNegativeButton(android.R.string.cancel,null).show())
                .setNeutralButton("Restaurar anterior",(d,i)->restore(a,done))
                .setNegativeButton(android.R.string.cancel,null).show();
        }catch(Exception e){error(a);}
    }
    private static final String[] VISUAL={"background_color","columns","rows_below_dock","dock_opacity","dock_glass","dock_title","dock_even_padding","ambient_glow","app_glow","header_white","fade_rows","focus_overshoot","advanced_effects","animations","reduced_motion","wallpaper_dim","wallpaper_true_color"};
    private static void write(AtomicFile file,byte[] data)throws IOException{
        FileOutputStream out=file.startWrite();
        try{out.write(data);file.finishWrite(out);}catch(IOException e){file.failWrite(out);throw e;}
    }
    private static void select(Activity a,String name,JSONObject theme,Runnable done){
        try {
            File dir=Config.dir(a);if(dir==null)throw new IOException();
            AtomicFile target=new AtomicFile(new File(dir,Config.FILE_NAME));
            AtomicFile eclipse=new AtomicFile(new File(a.getFilesDir(),"eclipse-original.json"));
            byte[] current=target.getBaseFile().exists()?target.readFully():"{}".getBytes(StandardCharsets.UTF_8);
            if(!eclipse.getBaseFile().exists())write(eclipse,current);
            if("Eclipse personalizado".equals(name)){
                write(new AtomicFile(new File(a.getFilesDir(),"previous-theme.json")),current);
                write(target,eclipse.readFully());
                done.run();
            }else apply(a,theme,done);
        }catch(Exception e){error(a);}
    }
    private static void apply(Activity a,JSONObject theme,Runnable done){
        try{
            File dir=Config.dir(a);if(dir==null)throw new IOException();
            AtomicFile target=new AtomicFile(new File(dir,Config.FILE_NAME));
            AtomicFile backup=new AtomicFile(new File(a.getFilesDir(),"previous-theme.json"));
            byte[] old=target.getBaseFile().exists()?target.readFully():"{}".getBytes(StandardCharsets.UTF_8);
            JSONObject current=new JSONObject(new String(old,StandardCharsets.UTF_8));
            for(String k:VISUAL){current.remove(k);if(theme.has(k))current.put(k,theme.get(k));}
            write(backup,old);
            write(target,current.toString().getBytes(StandardCharsets.UTF_8));
            a.getSharedPreferences(Config.SETTINGS_NAME,0).edit().remove("columns").remove(Config.ROWS_BELOW_DOCK_KEY).remove(Config.DOCK_OPACITY_KEY).remove("animations").remove("advanced_effects").commit();
            done.run();
        }catch(Exception e){error(a);}
    }
    private static void restore(Activity a,Runnable done){
        try{
            File dir=Config.dir(a);if(dir==null)throw new IOException();
            AtomicFile backup=new AtomicFile(new File(a.getFilesDir(),"previous-theme.json"));
            write(new AtomicFile(new File(dir,Config.FILE_NAME)),backup.readFully());
            done.run();
        }catch(Exception e){error(a);}
    }
    private static void error(Activity a){Toast.makeText(a,"No se pudo cambiar el tema",Toast.LENGTH_LONG).show();}
}
