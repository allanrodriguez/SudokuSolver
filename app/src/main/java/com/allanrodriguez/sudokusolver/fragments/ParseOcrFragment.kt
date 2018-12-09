package com.allanrodriguez.sudokusolver.fragments

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import com.allanrodriguez.sudokusolver.R
import com.allanrodriguez.sudokusolver.viewmodels.EnterPuzzleViewModel
import java.io.File
import java.io.FileOutputStream
import java.io.Serializable
import java.lang.ref.WeakReference

class ParseOcrFragment : Fragment() {

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        val image: Serializable? = arguments?.getSerializable(IMAGE)
        val imageRect: Rect? = arguments?.getParcelable(IMAGE_RECT)
        val squareRect: Rect? = arguments?.getParcelable(SQUARE_RECT)

        if (image is File && imageRect != null && squareRect != null) {
            ParseOcrAsyncTask(WeakReference(this), image, imageRect, squareRect).execute()
        } else {
            Log.e(tag, "An error occurred parsing the arguments passed to the Fragment.")
            fragmentManager?.popBackStack()
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_parse_ocr, container, false)
    }

    private class ParseOcrAsyncTask(
            val fragment: WeakReference<Fragment>,
            val image: File,
            val imageRect: Rect,
            val squareRect: Rect) : AsyncTask<Void, Void, Array<IntArray>>() {

        private val tag = "ParseOcrAsyncTask"
        private val trainedData = "eng.traineddata"

        private lateinit var tessData: File

        init {
            fragment.get()?.context?.let {
                tessData = File(File(it.cacheDir, "tessdata"), trainedData)
            }
        }

        override fun doInBackground(vararg params: Void?): Array<IntArray> {
            copyTessDataToCache()
            val puzzle: Array<IntArray> = parseSudokuFromImage(cropImage(), tessData.parentFile.absolutePath)
            tessData.delete()

            return puzzle
        }

        override fun onPostExecute(result: Array<IntArray>?) {
            super.onPostExecute(result)
            result?.let {
                setOcrResultToPuzzle(it)
            }
            closeDialog()
        }

        private fun closeDialog() {
            fragment.get()?.fragmentManager?.popBackStack()
        }

        private fun copyTessDataToCache() {
            val tessDataParent: File = tessData.parentFile

            Log.d(tag, "Creating folder ${tessDataParent.absolutePath}...")
            if (!(tessDataParent.mkdirs() || tessDataParent.exists())) {
                Log.e(tag, "tessdata directory was not created!")
            }
            Log.d(tag, "tessdata directory created.")

            val buffer = ByteArray(8192)

            fragment.get()?.context?.assets?.open(trainedData)?.use { source ->
                FileOutputStream(tessData).use { target ->
                    while (source.read(buffer) > 0) {
                        target.write(buffer)
                    }
                }
            }
        }

        private fun cropImage(): Bitmap {
            val originalImage: Bitmap = BitmapFactory.decodeFile(image.absolutePath)
            val scalingFactor: Double = arrayOf(originalImage.width.toDouble() / imageRect.width(),
                    originalImage.height.toDouble() / imageRect.height())
                    .average()

            return Bitmap.createBitmap(originalImage,
                    (scalingFactor * squareRect.left).toInt() - 4, (scalingFactor * squareRect.top).toInt() - 4,
                    (scalingFactor * squareRect.width()).toInt() + 4, (scalingFactor * squareRect.height()).toInt() + 4)
        }

        private fun setOcrResultToPuzzle(sudoku: Array<IntArray>) {
            Log.i(tag, "Copying parsed puzzle to the enter puzzle view-model...")

            val puzzleFragment: Fragment = fragment.get()?.fragmentManager?.findFragmentByTag(EnterPuzzleFragment::class.java.simpleName) as Fragment
            val puzzleViewModel: EnterPuzzleViewModel = ViewModelProviders.of(puzzleFragment).get(EnterPuzzleViewModel::class.java)

            for (i: Int in 0..8) {
                for (j: Int in 0..8) {
                    puzzleViewModel.sudoku[i][j].apply {
                        wasValueSetByUser.value = false
                        cellValue.value = if (sudoku[i][j] > 0) sudoku[i][j].toString() else ""
                    }
                }
            }

            Log.i(tag, "Done copying parsed puzzle to the enter puzzle view-model.")
        }

        private external fun parseSudokuFromImage(image: Bitmap, pathToTrainedData: String): Array<IntArray>

        companion object {
            init {
                System.loadLibrary("parseocrjni")
            }
        }
    }

    companion object {
        private const val IMAGE: String = "IMAGE"
        private const val IMAGE_RECT: String = "IMAGE_RECT"
        private const val SQUARE_RECT: String = "SQUARE_RECT"

        @JvmStatic
        fun newInstance(image: File, imageRect: Rect, squareRect: Rect): ParseOcrFragment {
            return ParseOcrFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(IMAGE, image)
                    putParcelable(IMAGE_RECT, imageRect)
                    putParcelable(SQUARE_RECT, squareRect)
                }
            }
        }
    }
}
