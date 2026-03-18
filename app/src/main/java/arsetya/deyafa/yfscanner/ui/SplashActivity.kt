package arsetya.deyafa.yfscanner.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import arsetya.deyafa.yfscanner.MainActivity
import arsetya.deyafa.yfscanner.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        startAnimations()
    }

    private fun startAnimations() {
        // Gradient circle pulse
        val circleScaleX = ObjectAnimator.ofFloat(binding.gradientCircle, View.SCALE_X, 0.5f, 1.2f, 1f).apply { duration = 1200 }
        val circleScaleY = ObjectAnimator.ofFloat(binding.gradientCircle, View.SCALE_Y, 0.5f, 1.2f, 1f).apply { duration = 1200 }
        val circleAlpha = ObjectAnimator.ofFloat(binding.gradientCircle, View.ALPHA, 0f, 0.2f).apply { duration = 800 }

        // Icon animation
        val iconScaleX = ObjectAnimator.ofFloat(binding.splashIcon, View.SCALE_X, 0.3f, 1f).apply {
            duration = 800
            startDelay = 300
            interpolator = OvershootInterpolator(2f)
        }
        val iconScaleY = ObjectAnimator.ofFloat(binding.splashIcon, View.SCALE_Y, 0.3f, 1f).apply {
            duration = 800
            startDelay = 300
            interpolator = OvershootInterpolator(2f)
        }
        val iconAlpha = ObjectAnimator.ofFloat(binding.splashIcon, View.ALPHA, 0f, 1f).apply {
            duration = 500
            startDelay = 300
        }

        // Title animation
        val titleAlpha = ObjectAnimator.ofFloat(binding.splashTitle, View.ALPHA, 0f, 1f).apply {
            duration = 600
            startDelay = 700
        }
        val titleTransY = ObjectAnimator.ofFloat(binding.splashTitle, View.TRANSLATION_Y, 30f, 0f).apply {
            duration = 600
            startDelay = 700
        }

        // Tagline animation
        val taglineAlpha = ObjectAnimator.ofFloat(binding.splashTagline, View.ALPHA, 0f, 1f).apply {
            duration = 600
            startDelay = 1000
        }
        val taglineTransY = ObjectAnimator.ofFloat(binding.splashTagline, View.TRANSLATION_Y, 20f, 0f).apply {
            duration = 600
            startDelay = 1000
        }

        // Developer credit animation
        val developerAlpha = ObjectAnimator.ofFloat(binding.splashDeveloper, View.ALPHA, 0f, 0.7f).apply {
            duration = 600
            startDelay = 1200
        }
        val developerTransY = ObjectAnimator.ofFloat(binding.splashDeveloper, View.TRANSLATION_Y, 15f, 0f).apply {
            duration = 600
            startDelay = 1200
        }

        // Progress animation
        val progressAlpha = ObjectAnimator.ofFloat(binding.splashProgress, View.ALPHA, 0f, 1f).apply {
            duration = 400
            startDelay = 1300
        }

        AnimatorSet().apply {
            playTogether(
                circleScaleX, circleScaleY, circleAlpha,
                iconScaleX, iconScaleY, iconAlpha,
                titleAlpha, titleTransY,
                taglineAlpha, taglineTransY,
                developerAlpha, developerTransY,
                progressAlpha
            )
            start()
        }

        // Navigate to main after delay
        binding.root.postDelayed({
            val intent = Intent(this@SplashActivity, MainActivity::class.java)
            val options = ActivityOptionsCompat.makeCustomAnimation(
                this@SplashActivity,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            startActivity(intent, options.toBundle())
            finish()
        }, 2500)
    }
}
