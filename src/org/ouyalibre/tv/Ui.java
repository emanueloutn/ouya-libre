package org.ouyalibre.tv;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;

public final class Ui {
 public static void controller(final android.app.Dialog d){d.setOnKeyListener(new DialogInterface.OnKeyListener(){public boolean onKey(DialogInterface dialog,int code,KeyEvent e){if(code==KeyEvent.KEYCODE_BUTTON_A){return d.getWindow().getDecorView().dispatchKeyEvent(new KeyEvent(e.getDownTime(),e.getEventTime(),e.getAction(),KeyEvent.KEYCODE_DPAD_CENTER,0));}if(code==KeyEvent.KEYCODE_BUTTON_B){if(e.getAction()==KeyEvent.ACTION_UP)d.cancel();return true;}return false;}});}
 public static final int BG=Color.rgb(13,20,31),TEXT=Color.rgb(232,240,247),ACCENT=Color.rgb(64,220,190);
 public static int dp(Context c,int n){return (int)(c.getResources().getDisplayMetrics().density*n+.5f);}
 public static TextView text(Context c,String s,int size){TextView t=new TextView(c);t.setText(s);t.setTextColor(TEXT);t.setTextSize(size);t.setPadding(12,8,12,8);return t;}
 public static Button button(Context c,String s,View.OnClickListener l){Button b=new Button(c);b.setText(s);b.setTextSize(16);b.setTextColor(TEXT);b.setFocusable(true);b.setOnClickListener(l);StateListDrawable d=new StateListDrawable();d.addState(new int[]{android.R.attr.state_focused},shape(Color.rgb(25,106,96)));d.addState(new int[]{android.R.attr.state_pressed},shape(Color.rgb(25,106,96)));d.addState(new int[]{},shape(Color.rgb(30,44,60)));b.setBackgroundDrawable(d);b.setPadding(14,6,14,6);return b;}
 public static GradientDrawable shape(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(8);d.setStroke(2,Color.rgb(65,92,110));return d;}
 public interface Input{void done(String value);}
 public static void keyboard(final Context c,String title,String initial,final Input callback){
  final android.app.Dialog dialog=new android.app.Dialog(c);controller(dialog);dialog.setTitle(title);LinearLayout root=new LinearLayout(c);root.setOrientation(1);root.setPadding(16,12,16,12);root.setBackgroundColor(BG);
  final EditText edit=new EditText(c);edit.setSingleLine(true);edit.setText(initial);edit.setTextColor(TEXT);root.addView(edit,new LinearLayout.LayoutParams(-1,dp(c,48)));
  final boolean[] caps={false};final java.util.List<Button> letters=new java.util.ArrayList<Button>();String[] rows={"abcdefghij","klmnopqrst","uvwxyzñ012","3456789.-_","/:?=&%+@"};
  for(String row:rows){LinearLayout line=new LinearLayout(c);for(int i=0;i<row.length();i++){final String ch=row.substring(i,i+1);Button b=button(c,ch,new View.OnClickListener(){public void onClick(View v){edit.append(caps[0]?ch.toUpperCase(java.util.Locale.US):ch);}});letters.add(b);line.addView(b,new LinearLayout.LayoutParams(0,dp(c,44),1));}root.addView(line);}
  LinearLayout actions=new LinearLayout(c);String[] labels={"Mayús","Espacio","Borrar","Limpiar","Aceptar","Cancelar"};for(final String label:labels){Button b=button(c,label,new View.OnClickListener(){public void onClick(View v){if(label.equals("Mayús")){caps[0]=!caps[0];for(Button key:letters){String s=key.getText().toString();key.setText(caps[0]?s.toUpperCase(java.util.Locale.US):s.toLowerCase(java.util.Locale.US));}}else if(label.equals("Espacio"))edit.append(" ");else if(label.equals("Borrar")){String s=edit.getText().toString();if(s.length()>0)edit.setText(s.substring(0,s.length()-1));}else if(label.equals("Limpiar"))edit.setText("");else if(label.equals("Aceptar")){String s=edit.getText().toString().trim();dialog.dismiss();callback.done(s);}else dialog.dismiss();}});actions.addView(b,new LinearLayout.LayoutParams(0,dp(c,48),1));}root.addView(actions);dialog.setContentView(root);dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);dialog.show();dialog.getWindow().setLayout(dp(c,720),-2);actions.getChildAt(4).requestFocus();
 }
}
