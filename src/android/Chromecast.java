package acidhax.cordova.chromecast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import com.google.android.gms.cast.*;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.json.JSONArray;
import org.json.JSONException;

import android.app.Activity;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter.RouteInfo;

public class Chromecast extends CordovaPlugin {
	
    private MediaRouter mMediaRouter;
    private MediaRouteSelector mMediaRouteSelector;
    private final ChromecastMediaRouterCallback mMediaRouterCallback = new ChromecastMediaRouterCallback();
    
    private ChromecastSession currentSession;
    

    public void initialize(final CordovaInterface cordova, CordovaWebView webView) {
    	super.initialize(cordova, webView);
    }
    
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext cbContext) throws JSONException {
    	try {
    		Method[] list = this.getClass().getMethods();
    		Method methodToExecute = null;
    		for (Method method : list) {
    			if (method.getName().equals(action)) {
    				Type[] types = method.getGenericParameterTypes();
    				if (args.length() + 1 == types.length) { // +1 is the cbContext
    					boolean isValid = false;
        				for (int i = 0; i < args.length(); i++) {
            				Class arg = args.get(i).getClass();
            				if (types[i] == arg) {
            					isValid = true;
            				} else {
            					isValid = false;
            					break;
            				}
        				}
        				if (isValid) {
            				methodToExecute = method;
            				break;
        				}
    				}
    			}
    		}
    		if (methodToExecute != null) {
    			Type[] types = methodToExecute.getGenericParameterTypes();
    			Object[] variableArgs = new Object[types.length];
    			for (int i = 0; i < args.length(); i++) {
    				variableArgs[i] = args.get(i);
    			}
    			variableArgs[variableArgs.length-1] = cbContext;
        		Class<?> r = methodToExecute.getReturnType();
        		if (r == boolean.class) {
            		return (Boolean) methodToExecute.invoke(this, variableArgs);
        		} else {
        			methodToExecute.invoke(this, variableArgs);
        			return true;
        		}
    		} else {
    			return false;
    		}
		} catch (IllegalAccessException e) {
            e.printStackTrace();
            return false;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return false;
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Execute function - Just echos - yay test
     * @param args
     * @param cbContext
     * @return
     * @throws JSONException
     */
    public boolean echo (String echo, CallbackContext cbContext) throws JSONException {
    	cbContext.success(echo);
    	return true;
    }
    
    /**
     * Execute function - Begins populating available Chromecast devices for a given appId 
     * @param args
     * @param cbContext
     * @return
     */
    public boolean getDevices (final String appId, final CallbackContext cbContext) throws JSONException {
    	final Activity activity = cordova.getActivity();
        final Chromecast that = this;
        activity.runOnUiThread(new Runnable() {
        	public void run() {
        		mMediaRouter = MediaRouter.getInstance(activity.getApplicationContext());
        		mMediaRouteSelector = new MediaRouteSelector.Builder()
        		.addControlCategory(CastMediaControlIntent.categoryForCast(appId))
        		.build();
        		mMediaRouterCallback.registerCallbacks(that);
        		mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
        		cbContext.success();
        	}
        });
        for (RouteInfo route : mMediaRouterCallback.getRoutes()) {
            that.webView.sendJavascript("chromecast.emit('device', '"+route.getId()+"', '" + route.getName() + "')");
        }
        return true;
    }
    
    /**
     * Execute function - Returns more information on a specific Chromecast device
     * TODO: Make it work
     * @param args
     * @param cbContext
     * @throws JSONException
     */
    public boolean getRoute(final String index, final CallbackContext cbContext) throws JSONException {
    	final Activity activity = cordova.getActivity();

        activity.runOnUiThread(new Runnable() {
        	public void run() {
            	RouteInfo info = mMediaRouterCallback.getRoute(index);
                if (info != null) {
                    cbContext.success(info.getName());    
                } else {
                    cbContext.error("No route found in " + mMediaRouterCallback.getRoutes().size() + " route(s)");
                }
        	}
        });
    	
        return true;
    }
    
    /**
     * Execute function - Launches a specific AppId on a Chromecast device
     * @param args
     * @param cbContext
     * @return
     * @throws JSONException
     */
    public boolean launch (String deviceId, String appId, CallbackContext callbackContext) throws JSONException {
    	RouteInfo info = mMediaRouterCallback.getRoute(id);
    	
    	if (this.currentSession == null) {
    		if (info != null) {
    			this.currentSession = new ChromecastSession(info, this.cordova);
    			
    			// Launch the app.
    			// TODO: Create a callback interface so we don't have to throw around the CallbackContext
    			this.currentSession.launch(appId, callbackContext);
    		} else {
    			callbackContext.error("No route found by that ID");
    		}
    	} else {
    		callbackContext.error("Already have a session, disconnect first");
    	}
    	
    	return true;
    }
    
    /**
     * Kill the session. Stops as well.
     * @param args
     * @param callbackContext
     * @return
     */
    public boolean kill (CallbackContext callbackContext) {
    	if (this.currentSession != null) {
    		this.currentSession.kill(callbackContext);
    		this.currentSession = null;
            callbackContext.success();
    	}
    	return true;
    }
    
    /**
     * Execute function - Loads a URL in an app that supports the Chromecast MediaReceiver
     * @param args
     * @param cbContext
     * @return
     * @throws JSONException
     */
    public boolean loadUrl(String url, final CallbackContext callbackContext) throws JSONException {
    	return this.currentSession.loadUrl(url, callbackContext);
    }
    
    
    public boolean play(CallbackContext callbackContext) {
    	currentSession.play(callbackContext);
    	return true;
    }
    
    public boolean pause(CallbackContext callbackContext) {
    	currentSession.pause(callbackContext);
    	return true;
    }
    
    public boolean stop(CallbackContext callbackContext) {
    	currentSession.stop(callbackContext);
    	return true;
    }
    
    public boolean seek(long seekTime, CallbackContext callbackContext) throws JSONException {
    	currentSession.seek(seekTime, callbackContext);
    	return true;
    }
    
    public boolean setVolume(double volume, CallbackContext callbackContext) throws JSONException {
    	currentSession.setVolume(volume, callbackContext);
    	return true;
    }
    
	
	protected void onRouteAdded(MediaRouter router, final RouteInfo route) {
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    Thread.sleep(500);
                    Chromecast.this.webView.sendJavascript("chromecast.emit('device', '"+route.getId()+"', '" + route.getName() + "')");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            
        }).start();
    }

	protected void onRouteRemoved(MediaRouter router, RouteInfo route) {
		this.webView.sendJavascript("chromecast.emit('deviceRemoved', '"+route.getId()+"', '" + route.getName() + "')");
	}

	protected void onRouteSelected(MediaRouter router, RouteInfo route) {
		this.webView.sendJavascript("chromecast.emit('routeSelected', '"+route.getId()+"', '" + route.getName() + "')");
	}

	protected void onRouteUnselected(MediaRouter router, RouteInfo route) {
		this.webView.sendJavascript("chromecast.emit('routeUnselected', '"+route.getId()+"', '" + route.getName() + "')");
	}
}

