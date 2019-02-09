describe('search non-existent author', function () {
  it('shows no result', async function () {
    const page = await browser.newPage();
    await page.goto('http://localhost:9000', {timeout: 0, waitUntil: 'domcontentloaded'});
    await page.type('div#summary input', 'AAA%AAsAABAA$AAnAACAA-AA(AADAA;AA)');
    await page.keyboard.press('Enter'); 
    const results = await page.evaluate(() => document.querySelector('#summary-charts').innerHTML);
    
    expect(results).to.be.empty;
  });
});
