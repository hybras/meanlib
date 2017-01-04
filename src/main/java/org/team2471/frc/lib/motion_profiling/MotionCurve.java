package org.team2471.frc.lib.motion_profiling;

import org.team2471.frc.lib.vector.Vector2;

public class MotionCurve {
  private final double MAXFRAMEERROR = 0.003;
  private MotionKey m_headKey;
  private MotionKey m_tailKey;
  private double m_defaultValue;
  private double m_minValue;
  private double m_maxValue;
  private double m_lastValue;
  private double m_lastTime;
  private boolean m_bLastTimeValid;
  private MotionKey m_lastAccessedKey;
  private ExtrapolationMethods m_preExtrapolation;
  private ExtrapolationMethods m_postExtrapolation;
  public MotionCurve() {
    m_headKey = null;
    m_tailKey = null;
    m_defaultValue = 0;
    m_minValue = -Double.MAX_VALUE;
    m_maxValue = Double.MAX_VALUE;
    m_lastValue = 0;
    m_lastTime = 0;
    m_bLastTimeValid = false;
    m_lastAccessedKey = null;
    m_preExtrapolation = ExtrapolationMethods.EXTRAPOLATION_CONSTANT;
    m_postExtrapolation = ExtrapolationMethods.EXTRAPOLATION_CONSTANT;
  }

  public MotionKey getHeadKey() {
    return m_headKey;
  }

  public void setHeadKey(MotionKey headKey) {
    this.m_headKey = headKey;
  }

  public MotionKey getTailKey() {
    return m_tailKey;
  }

  public void setTailKey(MotionKey tailKey) {
    this.m_tailKey = tailKey;
  }

  public double getLastTime() {
    return m_lastTime;
  }

  public void setLastTime(double m_lastTime) {
    this.m_lastTime = m_lastTime;
  }

  public boolean isLastTimeValid() {
    return m_bLastTimeValid;
  }

  public void setLastTimeValid(boolean lastTimeValid) {
    this.m_bLastTimeValid = lastTimeValid;
  }

  public double getDefaultValue() {
    return m_defaultValue;
  }

  public void setDefaultValue(double defaultValue) {
    this.m_defaultValue = defaultValue;
  }

  public double getMinValue() {
    return m_minValue;
  }

  public void setMinValue(double m_minValue) {
    this.m_minValue = m_minValue;
  }

  public double getMaxValue() {
    return m_maxValue;
  }

  public void setMaxValue(double m_maxValue) {
    this.m_maxValue = m_maxValue;
  }

  public MotionKey getLastAccessedKey() {
    return m_lastAccessedKey;
  }

  public void setLastAccessedKey(MotionKey m_lastAccessedKey) {
    this.m_lastAccessedKey = m_lastAccessedKey;
  }

  public double getLength() {
    return getTailKey() != null ? getTailKey().getTime() : 0;
  }

  private void insertKeyBefore(MotionKey atKey, MotionKey newKey) {
    newKey.setMotionCurve(this);

    if (atKey == m_headKey) {
      m_headKey = newKey;
      if (m_tailKey == null)
        m_tailKey = m_headKey;
    }

    m_lastAccessedKey = newKey;
    m_bLastTimeValid = false;

    if (atKey != null) {
      if (newKey.getTime() > atKey.getTime()) {
        System.out.println("Out of order keys detected.");
      }
      newKey.insertBefore(atKey);
    }

    newKey.onPositionChanged();
  }

  private void insertKeyAfter(MotionKey atKey, MotionKey newKey) {
    newKey.setMotionCurve(this);

    if (atKey == m_tailKey) {
      m_tailKey = newKey;
      if (m_headKey == null)
        m_headKey = m_tailKey;
    }

    m_lastAccessedKey = newKey;
    m_bLastTimeValid = false;

    if (atKey != null) {
      assert (newKey.getTime() >= atKey.getTime());
      newKey.insertAfter(atKey);
    }

    newKey.onPositionChanged();
  }

  void addKey(MotionKey newKey)  // adds the key to the end
  {
    insertKeyAfter(m_tailKey, newKey);
  }

  public void onKeyPositionChanged(MotionKey key) {
    setLastTimeValid(false);
  }

  private MotionKey findClosestKey(double time) {
    MotionKey pKey;
    if (m_lastAccessedKey != null)
      pKey = m_lastAccessedKey;
    else
      pKey = m_headKey;

    if (pKey == null)
      return null;

    if (pKey.getTime() != time) {
      if (pKey.getTime() < time) {
        MotionKey pNextKey = pKey.getNextKey();
        while (pNextKey != null) {
          if (pNextKey.getTime() > time)
            break;
          pKey = pNextKey;
          pNextKey = pKey.getNextKey();
        }
      } else {
        MotionKey pPrevKey = pKey.getPrevKey();
        while (pPrevKey != null) {
          pKey = pPrevKey;
          if (pKey.getTime() <= time)
            break;
          pPrevKey = pKey.getPrevKey();
        }
      }
    }

    m_lastAccessedKey = pKey;
    m_bLastTimeValid = false;

    return pKey;
  }

  public MotionKey getKey(double time) {
    MotionKey pKey;
    if (m_lastAccessedKey != null)
      pKey = m_lastAccessedKey;
    else
      pKey = m_headKey;

    if (pKey == null)
      return null;

    if (pKey.getTime() != time) {
      if (pKey.getTime() < time) {
        for (pKey = pKey.getNextKey(); pKey != null && pKey.getTime() != time; pKey = pKey.getNextKey())
          ;
      } else {
        for (pKey = pKey.getPrevKey(); pKey != null && pKey.getTime() != time; pKey = pKey.getPrevKey())
          ;
      }
    }

    if (pKey != null && m_lastAccessedKey != pKey) {
      m_lastAccessedKey = pKey;
      m_bLastTimeValid = false;
    }

    return pKey;
  }

  private MotionKey createMotionKey(double time) {
    MotionKey pKey = findClosestKey(time);
    MotionKey pNewKey;

    if (pKey != null && pKey.getTime() == time)
      pNewKey = pKey;
    else {
      pNewKey = new MotionKey();
      pNewKey.setMotionCurve(this);
      pNewKey.setTime(time);

      if (pKey == null)
        addKey(pNewKey);
      else if (pKey.getTime() <= time)
        insertKeyAfter(pKey, pNewKey);
      else if (pKey.getTime() > time)
        insertKeyBefore(pKey, pNewKey);
    }

    // for motion profiling, we want the first and last keys to be 0 slope, but all others to be normally smooth
    if (pNewKey == m_headKey) {
      pNewKey.setNextSlopeMethod(MotionKey.SlopeMethod.SLOPE_FLAT);
      if (pNewKey.getNextKey() != null && pNewKey.getNextKey() != m_tailKey) {  // the former head is not also the tail
        pNewKey.getNextKey().setNextSlopeMethod(MotionKey.SlopeMethod.SLOPE_SMOOTH);
        pNewKey.getNextKey().setPrevSlopeMethod(MotionKey.SlopeMethod.SLOPE_SMOOTH);
      }
    } else if (pNewKey == m_tailKey) {
      pNewKey.setPrevSlopeMethod(MotionKey.SlopeMethod.SLOPE_FLAT);
      if (pNewKey.getPrevKey() != null && pNewKey.getPrevKey() != m_headKey) {  // the former tail is not also the head
        pNewKey.getPrevKey().setNextSlopeMethod(MotionKey.SlopeMethod.SLOPE_SMOOTH);
        pNewKey.getPrevKey().setPrevSlopeMethod(MotionKey.SlopeMethod.SLOPE_SMOOTH);
      }
    } else {
      pNewKey.setNextSlopeMethod(MotionKey.SlopeMethod.SLOPE_SMOOTH);
      pNewKey.setPrevSlopeMethod(MotionKey.SlopeMethod.SLOPE_SMOOTH);
    }

    m_lastAccessedKey = pNewKey;
    m_bLastTimeValid = false;

    return pNewKey;
  }

  public MotionKey storeValue(double time, double value) {
    MotionKey motionKey = createMotionKey(time);
    if (motionKey != null)
      motionKey.setValue(value);
    return motionKey;
  }

  public double getValue(double time) {
    if (getHeadKey() == null)
      return getDefaultValue();

    // post-extrapolation
    if (time > getTailKey().getTime()) {
      switch (m_postExtrapolation) {
        case EXTRAPOLATION_CONSTANT:
          return getTailKey().getValue();
        case EXTRAPOLATION_LINEAR: {
          Vector2 v2Slope = getTailKey().getPrevTangent();
          return getTailKey().getValue() + (v2Slope.y / v2Slope.x) * (time - getTailKey().getTime());
        }
        case EXTRAPOLATION_CYCLE: {
          double tStartdouble = getHeadKey().getTime();
          double tLength = getTailKey().getTime() - tStartdouble;
          if (tLength != 0) {
            double tdoubleSinceStart = time - tStartdouble;
            double tdoubleInto = tdoubleSinceStart % tLength;
            double tNewdouble = tStartdouble - tdoubleInto;
            return getValue(tNewdouble);
          } else
            return getValue(tStartdouble);
        }
        case EXTRAPOLATION_CYCLE_RELATIVE: {
          double tStartdouble = getHeadKey().getTime();
          double tLength = getTailKey().getTime() - tStartdouble;
          if (tLength != 0) {
            double tdoubleSinceStart = time - tStartdouble;
            double tdoubleInto = tdoubleSinceStart % tLength;
            double tNewdouble = tStartdouble + tdoubleInto;

            int nCount = (int) (tdoubleSinceStart / tLength);
            double fHeight = getTailKey().getValue() - getHeadKey().getValue();
            return fHeight * nCount + getValue(tNewdouble);
          } else
            return getValue(tStartdouble);
        }
        case EXTRAPOLATION_OSCILLATE: {
          double tStartdouble = getHeadKey().getTime();
          double tLength = getTailKey().getTime() - tStartdouble;
          if (tLength != 0) {
            double tdoubleSinceStart = time - tStartdouble;
            double tdoubleInto = tdoubleSinceStart % tLength;
            double tNewdouble = tStartdouble + tdoubleInto;

            int nCount = (int) (tdoubleSinceStart / tLength);
            if (nCount % 2 != 0)
              return getValue(tLength - tNewdouble);
            else
              return getValue(tNewdouble);
          } else
            return getValue(tStartdouble);
        }
      }
    }

    // pre-extrapolation
    if (time < getHeadKey().getTime()) {
      switch (m_preExtrapolation) {
        case EXTRAPOLATION_CONSTANT:
          return getHeadKey().getValue();
        case EXTRAPOLATION_LINEAR: {
          Vector2 v2Slope = getHeadKey().getNextTangent();
          return getHeadKey().getValue() + (v2Slope.y / v2Slope.x) * (time - getHeadKey().getTime());
        }
        case EXTRAPOLATION_CYCLE: {
          double tStartdouble = getHeadKey().getTime();
          double tEnddouble = getTailKey().getTime();
          double tLength = tEnddouble - tStartdouble;
          if (tLength != 0) {
            double tdoubleSinceEnd = tEnddouble - time;
            double tdoubleInto = tdoubleSinceEnd % tLength;
            double tNewdouble = tEnddouble - tdoubleInto;
            return getValue(tNewdouble);
          } else
            return getValue(tStartdouble);
        }
        case EXTRAPOLATION_CYCLE_RELATIVE: {
          double tStartdouble = getHeadKey().getTime();
          double tEnddouble = getTailKey().getTime();
          double tLength = tEnddouble - tStartdouble;
          if (tLength != 0) {
            double tdoubleSinceEnd = tEnddouble - time;
            double tdoubleInto = tdoubleSinceEnd % tLength;
            double tNewdouble = tEnddouble - tdoubleInto;

            int nCount = (int) (tdoubleSinceEnd / tLength);
            double fHeight = getHeadKey().getValue() - getTailKey().getValue();
            return fHeight * nCount + getValue(tNewdouble);
          } else
            return getValue(tStartdouble);
        }
        case EXTRAPOLATION_OSCILLATE: {
          double tStartdouble = getHeadKey().getTime();
          double tEnddouble = getTailKey().getTime();
          double tLength = tEnddouble - tStartdouble;
          if (tLength != 0) {
            double tdoubleSinceEnd = tEnddouble - time;
            double tdoubleInto = tdoubleSinceEnd % tLength;
            double tNewdouble = tEnddouble - tdoubleInto;

            int nCount = (int) (tdoubleSinceEnd / tLength);
            if (nCount % 2 != 0)
              return getValue(tLength - tNewdouble);
            else
              return getValue(tNewdouble);
          } else
            return getValue(tStartdouble);
        }
      }
    }

    if (getLastAccessedKey() != null) {
      if (isLastTimeValid() && time == getLastTime())
        return m_lastTime; // if same as last time
    } else // if last key is not valid start from the beginning
    {
      setLastAccessedKey(getHeadKey());
    }

    if (getLastAccessedKey().getTime() <= time) {
      for (MotionKey key = getLastAccessedKey(); key != null; key = key.getNextKey()) {
        MotionKey nextKey = key.getNextKey();
        if (key.getTime() == time) {
          setLastAccessedKey(key);
          m_lastValue = key.getValue();
          break;
        } else if (nextKey.getTime() == time) {
          setLastAccessedKey(key);
          m_lastValue = nextKey.getValue();
          break;
        } else if (nextKey.getTime() > time) {
          setLastAccessedKey(key);
          m_lastValue = InterpolateValue(time, key);
          break;
        }
      }
    } else {
      for (MotionKey key = getLastAccessedKey().getPrevKey(); key != null; key = key.getPrevKey()) {
        MotionKey nextKey = key.getNextKey();
        if (key.getTime() == time) {
          setLastAccessedKey(key);
          m_lastValue = key.getValue();
          break;
        } else if (nextKey.getTime() == time) {
          setLastAccessedKey(key);
          m_lastValue = nextKey.getValue();
          break;
        } else if (key.getTime() < time) {
          setLastAccessedKey(key);
          m_lastValue = InterpolateValue(time, key);
          break;
        }
      }
    }

    setLastTime(time);
    setLastTimeValid(true);
    return m_lastValue;
  }

  private double InterpolateValue(double time, MotionKey pKey) {
    MotionKey pNextKey = pKey.getNextKey();

    MotionKey.SlopeMethod nextSlopeMethod = pKey.getNextSlopeMethod();
    MotionKey.SlopeMethod prevSlopeMethod = pNextKey.getPrevSlopeMethod();

    if (nextSlopeMethod == MotionKey.SlopeMethod.SLOPE_STEPPED)
      return pKey.getValue();
    else if (nextSlopeMethod == MotionKey.SlopeMethod.SLOPE_STEPPED_NEXT)
      return pNextKey.getValue();
    else if (nextSlopeMethod == MotionKey.SlopeMethod.SLOPE_LINEAR && prevSlopeMethod == MotionKey.SlopeMethod.SLOPE_LINEAR) {
      return pKey.getValue() + (time - pKey.getTime())
          / (pNextKey.getTime() - pKey.getTime())
          * (pNextKey.getValue() - pKey.getValue());
    } else {
      double evalx = time;
      double pointax = pKey.getTime();
      double pointbx = pNextKey.getTime();
      double xspan = pointbx - pointax;
      double guesst = (evalx - pointax) / xspan;

      // if the weights are default, then the x cubic is linear and there is no need to evaluate it
      if (pKey.getNextMagnitude() == 1.0f && pNextKey.getPrevMagnitude() == 1.0f)
        return pKey.getYCoefficients().Evaluate(guesst);

      // Spline - non default tangents means that we need a second parametric cubic for x as a function of t
      double diffx = evalx - pKey.getXCoefficients().Evaluate(guesst);
      double error = Math.abs(diffx);
      double maxerror = MAXFRAMEERROR / 30.0f;

      if (error > maxerror) {
        double positiveError = Double.MAX_VALUE;
        double negativeError = -Double.MAX_VALUE;

        if (diffx > 0)
          positiveError = diffx;
        else
          negativeError = diffx;

        while (error > maxerror) {
          guesst = guesst + diffx / pKey.getXCoefficients().Derivative(guesst);
          diffx = evalx - pKey.getXCoefficients().Evaluate(guesst);
          error = Math.abs(diffx);

          if ((diffx > 0 && diffx > positiveError) || (diffx < 0 && diffx < negativeError)) {  // NOT CONVERGING, PROBABLY BOGUS CHANNEL DATA, WALK USING BUMP FD
            assert (false);
            maxerror = 1.0f / 100.0f;  // DON'T BE AS ACCURATE BECAUSE THIS IS MUCH SLOWER
            int steps = (int) (xspan / maxerror);
            steps = Math.min(steps, 1000);
            double deltat = 1.0f / steps;
            pKey.getXCoefficients().InitFD(steps);
            int i;
            diffx = error;
            for (i = 0, guesst = 0.0; diffx > maxerror && i < steps; guesst += deltat, i++)
              diffx = Math.abs(evalx - pKey.getXCoefficients().BumpFD());
            break;
          }

          if (diffx > 0)
            positiveError = diffx;
          else
            negativeError = diffx;
        }
      }

      return pKey.getYCoefficients().Evaluate(guesst);
    }
  }

  public enum ExtrapolationMethods {EXTRAPOLATION_CONSTANT, EXTRAPOLATION_LINEAR, EXTRAPOLATION_CYCLE, EXTRAPOLATION_CYCLE_RELATIVE, EXTRAPOLATION_OSCILLATE}
}
