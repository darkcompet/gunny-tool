# Gunny Tool

Tool for Learning Gunny.


## How this project was made

- Create new project with: package name `compet.bundle`, min api 21, Kotlin, empty activity.

- At android studio, create default product flavor.

	```bash
	# Create resource folders
	Right click on `app` folder and choose `New -> Folder -> Res Folder`.

	# Create flavor with package name as `compet.gpscompass`
	- Select `Build -> Edit Flavors -> Build Variants -> app`
	- Add flavor dimension `defaultDimension`
	- Add product flavor `defaultFlavor`, and fill with below info:
		- Application id: `compet.gunnytool`
		- Version code: 1
		- Version name: 1.0.0
		- Target SDK version: 31
		- Min SDK version: 21
	
	# At `src` folder, create new empty activity at `defaultFlavor` to tell android studio
	# generate `defaultFlavor` folder for us.
	- Right click to `app` -> Select `New -> Activity -> Empty Activity`
	```

- Add darkcompet support modules

	```bash
	# At android modules
	git submodule add https://github.com/darkcompet/android-module-core.git
	git submodule add https://github.com/darkcompet/android-module-json.git

	# Import modules by add below lines to `settings.gradle` file
	include ':android-module-core'
	include ':android-module-json'

	# And at `app/build.gradle`, declare modules which project needs, for eg:
	implementation project(path: ':android-module-core')
	implementation project(path: ':android-module-json')
	```
