describe('search unique author', function () {
  it('shows one author', async function () {
    const page = await browser.newPage();
    await page.goto('http://localhost:9000', {timeout: 0, waitUntil: 'domcontentloaded'});
    await page.type('div#summary input', 'Yong Hao TENG');
    await page.keyboard.press('Enter');
    const results = await page.evaluate(() => document.querySelector('#summary-charts').children.length);

    expect(results).to.be.equal(1);
  });
});
