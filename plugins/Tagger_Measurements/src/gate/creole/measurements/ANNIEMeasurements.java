package gate.creole.measurements;

import gate.creole.PackagedController;
import gate.creole.metadata.AutoInstance;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;

import java.net.URL;
import java.util.List;

@CreoleResource(name = "ANNIE+Measurements", icon = "measurements", autoinstances = @AutoInstance)
public class ANNIEMeasurements extends PackagedController {

  private static final long serialVersionUID = 3163023140886167369L;

  @Override
  @CreoleParameter(defaultValue = "resources/annie-measurements.xgapp")
  public void setPipelineURL(URL url) {
    this.url = url;
  }

  @Override
  @CreoleParameter(defaultValue = "ANNIE")
  public void setMenu(List<String> menu) {
    super.setMenu(menu);
  }
}
