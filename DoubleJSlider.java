import javax.swing.*;

public class DoubleJSlider extends JSlider{
  
  static final double DOUBLE_MINIMUM = 0.0;
  static final double DOUBLE_MAXIMUM = 100.0;
  static final double DOUBLE_MIDDLE = 50.0;
  static final int PRECISION_MULTIPLIER  = 100;
  /**
   * Constructor - initializes with 0.0,100.0,50.0
   */
  public DoubleJSlider(){
    super();
    setDoubleMinimum(DOUBLE_MINIMUM);
    setDoubleMaximum(DOUBLE_MAXIMUM);
    setDoubleValue(DOUBLE_MIDDLE);
  }

  /**
   * Constructor
   */
  public DoubleJSlider(double min, double max, double val){
    super();
    setDoubleMinimum(min);
    setDoubleMaximum(max);
    setDoubleValue(val);
  }

  /**
   * returns Maximum in double precision
   */
  public double getDoubleMaximum() {
    return( getMaximum()/DOUBLE_MAXIMUM );
  }

  /**
   * returns Minimum in double precision
   */
  public double getDoubleMinimum() {
    return( getMinimum()/DOUBLE_MAXIMUM );
  }

  /**
   * returns Value in double precision
   */
  public double getDoubleValue() {
    return(getValue()/DOUBLE_MAXIMUM);
  }

  /**
   * sets Maximum in double precision
   */
  public void setDoubleMaximum(double max) {
    setMaximum((int)(max*PRECISION_MULTIPLIER));
  }

  /**
   * sets Minimum in double precision
   */
  public void setDoubleMinimum(double min) {
    setMinimum((int)(min*PRECISION_MULTIPLIER));
  }

  /**
   * sets Value in double precision
   */
  public void setDoubleValue(double val) {
    setValue((int)(val*PRECISION_MULTIPLIER));
    setToolTipText(Double.toString(val));
  }

}
