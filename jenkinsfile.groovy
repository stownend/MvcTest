node {
    
	def solutionPath = pwd()
	def solution = "MvcTest.sln"
	def buildType = "Release"
	def errorCode = null
	
	try
	{
		stage 'Checkout'
			echo 'Getting from git...'
			checkout scm
			//git 'https://github.com/stownend/MvcTest.git/'

		stage 'Build'
			echo 'Restoring nuget packages...'
			bat "c:/jenkins/extras/nuget.exe restore ${solution}"
			echo 'Building solution...'
			bat "\"${tool name: 'Vs2017', type: 'msbuild'}\" \"${solutionPath}\\${solution}\" /p:Configuration=${buildType} /p:Platform=\"Any CPU\" /p:ProductVersion=1.0.0.${env.BUILD_NUMBER}"

		stage 'Unit Test'
			echo 'Running unit tests...'
						
			bat "del /q TestResults\\*.trx"
			
			bat "\"${tool name: 'VS2017', type: 'org.jenkinsci.plugins.vstest_runner.VsTestInstallation'}\" /Enablecodecoverage /UseVsixExtensions:false /Logger:trx ${getTestAssemblies(buildType)}"
			
			
		stage 'Archive'
			//archive 'ProjectName/bin/Release/**'

		stage 'Deploy to staging'
			notify("Is ready to deploy to staging", "TestRoom", "PURPLE", "/input")
						
			Map feedback = input(submitterParameter: 'approver', message: "Deploy to staging?", parameters:[booleanParam(defaultValue: false, description: '', name: 'Deploy')] )

			notify("User Id: ${feedback.approver} approved deployment to staging", "TestRoom", "GREEN", "")
			
			//archive 'ProjectName/bin/Release/**'
			
	}
	catch(error)
	{
		echo "Err : ${error}"
		notify("Failed", "TestRoom", "RED", "")
		errorCode = error
	}
	finally
	{
		bat "c:/jenkins/extras/msxsl.exe \"" + solutionPath + "\\" + getTestResultFile() + "\" c:/jenkins/extras/mstest-to-junit.xsl -o JUnitLikeResultsOutputFile1.xml"
	
		step([$class: 'JUnitResultArchiver', allowEmptyResults: true, testResults: 'JUnitLikeResultsOutputFile1.xml'])

		if (errorCode != null)
		{
			throw errorCode
		}
			
	}

	if (currentBuild.previousBuild.result == "FAILURE")
	{
		notify("Back to normal", "TestRoom", "GREEN", "")
	}
	
}

def notify(status, room, colour, urlExtension) {
	hipchatSend (
		color: "${colour}", 
		credentialId: 'da34e58a-cc1c-44bc-8f75-932a36037f96', 
		message: '$JOB_NAME #$BUILD_NUMBER ' + status + ' ($HIPCHAT_CHANGES_OR_CAUSE) (<a href="$BUILD_URL' + urlExtension + '">View build</a>)', 
		room: "${room}", 
		sendAs: 'Jenkins', 
		server: 'api.hipchat.com', 
		v2enabled: false
	)
}

// Build a list of test dlls
def getTestAssemblies(buildProfile) {
	String unitTestAssemblies = bat returnStdout: true, script: '''@echo off
		@setlocal enabledelayedexpansion enableextensions
		@set list=
		@for /d /r %%x in (bin\\''' + buildProfile + '''*) do	(
				pushd %%x
				@for %%y in ("*.tests.dll") do set list=!list!  "%%x\\%%y"
				popd
			)			
		@set list=%list:~1%
		@echo %list%
		'''
		
	return unitTestAssemblies.trim()
}

// Build a list of test dlls
def getTestResultFile() {
	String unitTestResults = bat returnStdout: true, script: '''@echo off
		@setlocal enabledelayedexpansion enableextensions
		@set list=
		@for %%y in ("TestResults\\*.trx") do set list=!list! %%y
		@set list=%list:~1%
		@echo %list%
		'''

	return unitTestResults.trim()
}